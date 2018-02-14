/*
Copyright 2018 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package io.omam.halo;

import static io.omam.halo.MulticastDns.IPV4_ADDR;
import static io.omam.halo.MulticastDns.IPV4_SOA;
import static io.omam.halo.MulticastDns.IPV6_ADDR;
import static io.omam.halo.MulticastDns.IPV6_SOA;
import static io.omam.halo.MulticastDns.MAX_DNS_MESSAGE_SIZE;
import static io.omam.halo.MulticastDns.MDNS_PORT;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MulticastChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link DatagramChannel}(s) to send and receive {@link DnsMessage DNS message}s.
 */
final class HaloChannel implements AutoCloseable {

    /**
     * DNS message receiver.
     * <p>
     * {@link Consumer} given at construction is invoked whenever a message is received.
     */
    private final class Receiver implements Runnable {

        /**
         * Constructor.
         */
        Receiver() {
            // empty.
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void run() {
            final ByteBuffer buf = ByteBuffer.allocate(MAX_DNS_MESSAGE_SIZE);
            buf.order(ByteOrder.BIG_ENDIAN);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    selector.select();
                    LOGGER.fine("Channels ready for I/O operations");
                    final Set<SelectionKey> selected = selector.selectedKeys();
                    for (final SelectionKey key : selected) {
                        final DatagramChannel channel = (DatagramChannel) key.channel();
                        buf.clear();
                        final InetSocketAddress address = (InetSocketAddress) channel.receive(buf);
                        if (address != null && buf.position() != 0) {
                            buf.flip();
                            final byte[] bytes = new byte[buf.remaining()];
                            buf.get(bytes);
                            final DnsMessage msg = DnsMessage.decode(bytes, clock.instant());
                            LOGGER.fine(() -> "Received " + msg + " on " + address);
                            listener.accept(msg);
                        }
                    }
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "I/O error while receiving DNS message", e);
                }
            }
        }

    }

    /**
     * DNS message sender.
     * <p>
     * Messages are taken from the sending queue.
     */
    @SuppressWarnings("synthetic-access")
    private final class Sender implements Runnable {

        /**
         * Constructor.
         */
        Sender() {
            // empty.
        }

        @Override
        public final void run() {
            final ByteBuffer buf = ByteBuffer.allocate(MAX_DNS_MESSAGE_SIZE);
            buf.order(ByteOrder.BIG_ENDIAN);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final DnsMessage msg = sq.take();
                    LOGGER.fine(() -> "Sending " + msg);
                    final byte[] packet = msg.encode();
                    buf.clear();
                    buf.put(packet);
                    buf.flip();
                    ipv4.forEach(ni -> send(ni, buf, IPV4_SOA));
                    ipv6.forEach(ni -> send(ni, buf, IPV6_SOA));
                } catch (final InterruptedException e) {
                    LOGGER.log(Level.FINE, "Interrupted while waiting to DNS message", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Sends given datagram to given channel an address
         *
         * @param key channel
         * @param src the buffer containing the datagram to be sent
         * @param target the address to which the datagram is to be sent
         */
        private void send(final SelectionKey key, final ByteBuffer src, final InetSocketAddress target) {
            final int position = src.position();
            try {
                ((DatagramChannel) key.channel()).send(src, target);
                LOGGER.fine(() -> "Sent DNS message to " + target);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, e, () -> "I/O error while sending DNS message to " + target);
            } finally {
                src.position(position);
            }
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(HaloChannel.class.getName());

    /** clock. */
    private final Clock clock;

    /** executor service to send/receive messages. */
    private final ExecutorService es;

    /** IPV4 channel(s). */
    private final List<SelectionKey> ipv4;

    /** IPV6 channel(s). */
    private final List<SelectionKey> ipv6;

    /** listener to be invoked whenever a new message is received. */
    private final Consumer<DnsMessage> listener;

    /** future to cancel receiving messages. */
    private Future<?> receiver;

    /** multiplexor. */
    private final Selector selector;

    /** future to cancel sending messages. */
    private Future<?> sender;

    /** queue of sent DNS messages. */
    private final BlockingQueue<DnsMessage> sq;

    /**
     * Constructor.
     *
     * @param aListener listener to be invoked whenever a new message is received
     * @param aClock clock
     * @param nis network interfaces
     * @throws IOException if an I/O error occurs
     */
    private HaloChannel(final Consumer<DnsMessage> aListener, final Clock aClock,
            final Collection<NetworkInterface> nis) throws IOException {
        clock = aClock;
        es = Executors.newFixedThreadPool(2, new HaloThreadFactory("channel"));
        listener = aListener;
        selector = Selector.open();
        sq = new LinkedBlockingQueue<>();

        ipv4 = new ArrayList<>();
        ipv6 = new ArrayList<>();

        for (final NetworkInterface ni : nis) {
            openChannel(ni, StandardProtocolFamily.INET, false).map(this::register).ifPresent(ipv4::add);
            openChannel(ni, StandardProtocolFamily.INET6, false).map(this::register).ifPresent(ipv6::add);
        }

        if (ipv4.isEmpty() && ipv6.isEmpty()) {
            for (final NetworkInterface ni : nis) {
                LOGGER.info(() -> "No Network Interface found, adding Loopback interface");
                openChannel(ni, StandardProtocolFamily.INET, true).map(this::register).ifPresent(ipv4::add);
                openChannel(ni, StandardProtocolFamily.INET6, true).map(this::register).ifPresent(ipv6::add);
            }
        }

        if (ipv4.isEmpty() && ipv6.isEmpty()) {
            throw new IOException("No network interface suitable for multicast");
        }

    }

    /**
     * Creates a new channel sending/receiving on all interfaces on this machine.
     *
     * @param listener listener to be invoked whenever a new message is received
     * @param clock clock
     * @return a new channel
     * @throws IOException if an I/O error occurs
     */
    static HaloChannel allNetworkInterfaces(final Consumer<DnsMessage> listener, final Clock clock)
            throws IOException {
        final Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        final Collection<NetworkInterface> c = new ArrayList<>();
        while (nics.hasMoreElements()) {
            c.add(nics.nextElement());
        }
        return networkInterfaces(listener, clock, c);
    }

    /**
     * Creates a new channel sending/receiving on the given interfaces.
     *
     * @param listener listener to be invoked whenever a new message is received
     * @param clock clock
     * @param nics network interfaces
     * @return a new channel
     * @throws IOException if an I/O error occurs
     */
    static HaloChannel networkInterfaces(final Consumer<DnsMessage> listener, final Clock clock,
            final Collection<NetworkInterface> nics) throws IOException {
        return new HaloChannel(listener, clock, nics);
    }

    @Override
    public final synchronized void close() throws IOException {
        LOGGER.fine("Closing channel");
        selector.wakeup();
        disable();
        es.shutdownNow();
        try {
            close(ipv4);
        } finally {
            close(ipv6);
        }
    }

    /**
     * Enables receiving/sending DNS messages.
     */
    final synchronized void enable() {
        if (sender == null) {
            sender = es.submit(new Sender());
        }
        if (receiver == null) {
            receiver = es.submit(new Receiver());
        }
    }

    /**
     * Adds the given message to the queue of messages to send.
     *
     * @param message message to send
     */
    final void send(final DnsMessage message) {
        sq.add(message);
    }

    /**
     * Closes the given {@link SelectionKey}(s).
     *
     * @param keys selection keys
     * @throws IOException in case of I/O error
     */
    private void close(final List<SelectionKey> keys) throws IOException {
        IOException ex = null;
        for (final SelectionKey key : keys) {
            try {
                key.channel().close();
            } catch (final IOException e) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * Disables receiving/sending DNS messages.
     */
    private synchronized void disable() {
        if (sender != null) {
            sender.cancel(true);
        }
        if (receiver != null) {
            receiver.cancel(true);
        }
    }

    /**
     * Determines whether the given network interface has an address of the given class.
     *
     * @param ni network interface
     * @param ipv {@link InetAddress} class
     * @return {@code true} if given network interface has an address of the given class
     */
    private boolean hasIpv(final NetworkInterface ni, final Class<? extends InetAddress> ipv) {
        for (final Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
            if (e.nextElement().getClass().isAssignableFrom(ipv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens a new {@link MulticastChannel multicast channel} for the the given network interface.
     * <p>
     * A new channel is opened iff the given network interface {@link NetworkInterface#supportsMulticast() supports
     * multicast}, is {@link NetworkInterface#isUp() up} and has at least one address matching the given protocol
     * family
     *
     * @param ni all network interface
     * @param family IPV4 or IPV5
     * @param loopback {@code true} if given interface must be the loopback, {@code false} if it must not
     * @return a new multicast channel or empty if the given network interface is not valid
     */
    private Optional<DatagramChannel> openChannel(final NetworkInterface ni, final ProtocolFamily family,
            final boolean loopback) {
        final boolean ipv4Protocol = family == StandardProtocolFamily.INET;
        final InetAddress addr = ipv4Protocol ? IPV4_ADDR : IPV6_ADDR;
        try {
            final Class<? extends InetAddress> ipvClass = ipv4Protocol ? Inet4Address.class : Inet6Address.class;
            if (ni.supportsMulticast() && ni.isUp() && ni.isLoopback() == loopback && hasIpv(ni, ipvClass)) {
                final Optional<DatagramChannel> channel = openChannel(family);
                if (channel.isPresent()) {
                    channel.get().setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
                    channel.get().join(addr, ni);
                    LOGGER.info(() -> "Joined multicast address " + addr + " on " + ni);
                    return channel;
                }
            }
            LOGGER.fine(() -> "Ignored " + ni + " for " + addr);
            return Optional.empty();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "Ignored " + ni + " for " + addr);
            return Optional.empty();
        }
    }

    /**
     * Opens a new {@link DatagramChannel}.
     *
     * @param family the protocol family
     * @return a new datagram channel
     */
    private Optional<DatagramChannel> openChannel(final ProtocolFamily family) {
        try {
            final DatagramChannel channel = DatagramChannel.open(family);
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
            channel.bind(new InetSocketAddress(MDNS_PORT));
            return Optional.of(channel);
        } catch (final UnsupportedOperationException e) {
            LOGGER.log(Level.FINE, e,
                    () -> "Protocol Family [" + family.name() + "] not supported on this machine.");
            return Optional.empty();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Fail to create channel", e);
            return Optional.empty();
        }
    }

    /**
     * Registers the given channel with the {@link #selector} for read operation, returning a selection key.
     *
     * @param channel channel
     * @return selection key
     */
    private SelectionKey register(final DatagramChannel channel) {
        try {
            return channel.register(selector, SelectionKey.OP_READ);
        } catch (final ClosedChannelException e) {
            LOGGER.severe(() -> "Could not register channel with selector");
            throw new IllegalStateException(e);
        }
    }

}
