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
package net.omam.zeroconf;

import static net.omam.zeroconf.MulticastDns.IPV4_ADDR;
import static net.omam.zeroconf.MulticastDns.IPV4_SOA;
import static net.omam.zeroconf.MulticastDns.IPV6_ADDR;
import static net.omam.zeroconf.MulticastDns.IPV6_SOA;
import static net.omam.zeroconf.MulticastDns.MAX_DNS_MESSAGE_SIZE;
import static net.omam.zeroconf.MulticastDns.MDNS_PORT;

import java.io.Closeable;
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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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
final class ZeroconfChannel implements Closeable {

    /**
     * DNS message sender.
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

        @SuppressWarnings({ "synthetic-access", "resource" })
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
                    selected.clear();
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
                    if (ipv4.isPresent()) {
                        send(ipv4.get(), buf, IPV4_SOA);
                    }
                    if (ipv6.isPresent()) {
                        send(ipv6.get(), buf, IPV6_SOA);
                    }
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
            try {
                ((DatagramChannel) key.channel()).send(src, target);
                LOGGER.fine(() -> "Sent DNS message to " + target);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, e, () -> "I/O error while sending DNS message to " + target);
            }
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ZeroconfChannel.class.getName());

    /** clock. */
    private final Clock clock;

    /** executor service to send/receive messages. */
    private final ExecutorService es;

    /** IPV4 channel if available. */
    private final Optional<SelectionKey> ipv4;

    /** IPV6 channel if available. */
    private final Optional<SelectionKey> ipv6;

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
    private ZeroconfChannel(final Consumer<DnsMessage> aListener, final Clock aClock,
            final Collection<NetworkInterface> nis) throws IOException {
        clock = aClock;
        es = Executors.newFixedThreadPool(2);
        listener = aListener;
        selector = Selector.open();
        sq = new LinkedBlockingQueue<>();

        Optional<DatagramChannel> ipv4Channel = openChannel(StandardProtocolFamily.INET);
        Optional<DatagramChannel> ipv6Channel = openChannel(StandardProtocolFamily.INET6);

        if (!ipv4Channel.isPresent() && !ipv6Channel.isPresent()) {
            throw new IOException("Machine supports neither IPV4 or IPV6");
        }

        boolean ipv4Addr = false;
        boolean ipv6Addr = false;
        // final NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        // ipv4Addr |= ipv4Channel.map(c -> addNetworkInterface(c, ni, true, true)).orElse(false);
        // ipv6Addr |= ipv6Channel.map(c -> addNetworkInterface(c, ni, false, true)).orElse(false);

        for (final NetworkInterface ni : nis) {
            ipv4Addr |= ipv4Channel.map(c -> addNetworkInterface(c, ni, true, false)).orElse(false);
            ipv6Addr |= ipv6Channel.map(c -> addNetworkInterface(c, ni, false, false)).orElse(false);
        }

        if (!ipv4Addr && !ipv6Addr) {
            LOGGER.info(() -> "No Network Interface found, adding Loopback interface");
            ipv4Addr |= ipv4Channel.map(c -> addLoopbackInterface(c, nis, true)).orElse(false);
            ipv6Addr |= ipv6Channel.map(c -> addLoopbackInterface(c, nis, false)).orElse(false);
        }

        if (!ipv4Addr && !ipv6Addr) {
            throw new IOException("No IPV4 or IPV6 address found");
        }

        if (!ipv4Addr && ipv4Channel.isPresent()) {
            ipv4Channel.get().close();
            ipv4Channel = Optional.empty();
        }

        if (!ipv6Addr && ipv6Channel.isPresent()) {
            ipv6Channel.get().close();
            ipv6Channel = Optional.empty();
        }

        ipv4 = register(ipv4Channel);
        ipv6 = register(ipv6Channel);
    }

    /**
     * Creates a new channel sending/receiving on all interfaces on this machine.
     *
     * @param listener listener to be invoked whenever a new message is received
     * @param clock clock
     * @return a new channel
     * @throws IOException if an I/O error occurs
     */
    static ZeroconfChannel allNetworkInterfaces(final Consumer<DnsMessage> listener, final Clock clock)
            throws IOException {
        final Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        final Collection<NetworkInterface> c = new ArrayList<>();
        while (nics.hasMoreElements()) {
            c.add(nics.nextElement());
        }
        return new ZeroconfChannel(listener, clock, c);
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
    static ZeroconfChannel networkInterfaces(final Consumer<DnsMessage> listener, final Clock clock,
            final Collection<NetworkInterface> nics) throws IOException {
        return new ZeroconfChannel(listener, clock, nics);
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
     * @return {@code true} iff IPV4 is supported by this machine.
     */
    final boolean ipv4Supported() {
        return ipv4.isPresent();
    }

    /**
     * @return {@code true} iff IPV6 is supported by this machine.
     */
    final boolean ipv6Supported() {
        return ipv6.isPresent();
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
     * Adds the {@link NetworkInterface#isLoopback() loopback interface} to the given channel.
     *
     * @param channel channel
     * @param nis all network interface
     * @param ipv4Protocol {@code true} if loopback interface shall be added if it supports IPV4, {@code false} for
     *            IPV6
     * @return {@code true} if loopback interface was added to the channel
     */
    private boolean addLoopbackInterface(final DatagramChannel channel, final Collection<NetworkInterface> nis,
            final boolean ipv4Protocol) {
        for (final NetworkInterface ni : nis) {
            final boolean added = addNetworkInterface(channel, ni, ipv4Protocol, true);
            if (added) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the given network interface to the given channel.
     *
     * @param channel channel
     * @param ni all network interface
     * @param ipv4Protocol {@code true} if network interface shall be added if it supports IPV4, {@code false} for
     *            IPV6
     * @param loopback {@code true} if given interface must be the loopback, {@code false} if it must not
     * @return {@code true} if network interface was added to the channel
     */
    private boolean addNetworkInterface(final DatagramChannel channel, final NetworkInterface ni,
            final boolean ipv4Protocol, final boolean loopback) {
        final InetAddress addr = ipv4Protocol ? IPV4_ADDR : IPV6_ADDR;
        try {
            final Class<? extends InetAddress> ipvClass = ipv4Protocol ? Inet4Address.class : Inet6Address.class;
            if (ni.supportsMulticast() && ni.isUp() && ni.isLoopback() == loopback && hasIpv(ni, ipvClass)) {
                channel.join(addr, ni);
                channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
                LOGGER.info(() -> "Joined multicast address " + addr + " on " + ni);
                return true;
            }
            LOGGER.fine(() -> "Ignored " + ni + " for " + addr);
            return false;
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "Ignored " + ni + " for " + addr);
            return false;
        }
    }

    /**
     * Closes the given {@link SelectionKey} if present.
     *
     * @param key selection key
     * @throws IOException in case of I/O error
     */
    private void close(final Optional<SelectionKey> key) throws IOException {
        if (key.isPresent()) {
            key.get().channel().close();
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
     * Opens a new {@link DatagramChannel}.
     *
     * @param family the protocol family
     * @return a new datagram channel
     */
    @SuppressWarnings({ "resource", "unused" })
    private Optional<DatagramChannel> openChannel(final ProtocolFamily family) {
        try {
            final DatagramChannel channel = DatagramChannel.open(family);
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
            channel.bind(new InetSocketAddress(MDNS_PORT));
            return Optional.of(channel);
        } catch (final UnsupportedOperationException e) {
            LOGGER.fine(() -> "Protocol Family [" + family.name() + "] not supported on this machine.");
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
     * @throws ClosedChannelException if the channel is closed
     */
    private Optional<SelectionKey> register(final Optional<DatagramChannel> channel)
            throws ClosedChannelException {
        if (channel.isPresent()) {
            return Optional.of(channel.get().register(selector, SelectionKey.OP_READ));
        }
        return Optional.empty();
    }

}
