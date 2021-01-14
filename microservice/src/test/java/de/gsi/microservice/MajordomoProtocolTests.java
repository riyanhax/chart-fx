package de.gsi.microservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * basic MDP OpenCMW protocol consistency tests
 */
class MajordomoProtocolTests {
    private static final MajordomoProtocol.MdpMessage TEST_MESSAGE = new MajordomoProtocol.MdpMessage("senderName".getBytes(StandardCharsets.UTF_8), //
            MajordomoProtocol.MdpSubProtocol.PROT_CLIENT, MajordomoProtocol.Command.GET_REQUEST, //
            "serviceName".getBytes(StandardCharsets.UTF_8), new byte[] { (byte) 3, (byte) 2, (byte) 1 },
            URI.create("test/topic"), "test data - Hello World!".getBytes(StandardCharsets.UTF_8), "error message", "rbacToken".getBytes(StandardCharsets.UTF_8));

    @Test
    void testCommandEnum() {
        for (MajordomoProtocol.Command cmd : MajordomoProtocol.Command.values()) {
            assertEquals(cmd, MajordomoProtocol.Command.getCommand(cmd.getData()));
            assertNotNull(cmd.toString());
            if (!cmd.equals(MajordomoProtocol.Command.UNKNOWN)) {
                assertTrue(cmd.isClientCompatible() || cmd.isWorkerCompatible());
            }
        }
        assertFalse(MajordomoProtocol.Command.UNKNOWN.isWorkerCompatible());
        assertFalse(MajordomoProtocol.Command.UNKNOWN.isClientCompatible());
    }

    @Test
    void testMdpSubProtocolEnum() {
        for (MajordomoProtocol.MdpSubProtocol cmd : MajordomoProtocol.MdpSubProtocol.values()) {
            assertEquals(cmd, MajordomoProtocol.MdpSubProtocol.getProtocol(cmd.getData()));
            assertNotNull(cmd.toString());
        }
    }

    @Test
    void testMdpIdentity() {
        assertEquals(TEST_MESSAGE, TEST_MESSAGE, "object identity");
        assertNotEquals(TEST_MESSAGE, new Object(), "unequality if different class type");
        final MajordomoProtocol.MdpMessage clone = new MajordomoProtocol.MdpMessage(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, clone, "copy constructor");
        assertEquals(TEST_MESSAGE.hashCode(), clone.hashCode(), "hashCode equality");
        final MajordomoProtocol.MdpMessage modified = new MajordomoProtocol.MdpMessage(TEST_MESSAGE);
        modified.protocol = MajordomoProtocol.MdpSubProtocol.PROT_WORKER;
        assertNotEquals(TEST_MESSAGE, modified, "copy constructor");
        assertTrue(TEST_MESSAGE.hasRbackToken(), "non-empty rbac token");
        assertEquals("senderName", TEST_MESSAGE.getSenderName(), "sender name string");
        assertEquals("serviceName", TEST_MESSAGE.getServiceName(), "service name string");
        assertNotNull(TEST_MESSAGE.toString());
    }

    @Test
    void testMdpSendReceiveIdentity() {
        try (ZContext ctx = new ZContext()) {
            {
                final ZMQ.Socket receiveSocket1 = ctx.createSocket(SocketType.ROUTER);
                receiveSocket1.bind("inproc://pair1");
                final ZMQ.Socket receiveSocket2 = ctx.createSocket(SocketType.DEALER);
                receiveSocket2.bind("inproc://pair2");
                final ZMQ.Socket sendSocket = ctx.createSocket(SocketType.DEALER);
                sendSocket.setIdentity(TEST_MESSAGE.senderID);
                sendSocket.connect("inproc://pair1");
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));

                TEST_MESSAGE.send(sendSocket);
                final MajordomoProtocol.MdpMessage reply = MajordomoProtocol.MdpMessage.receive(receiveSocket1);
                assertEquals(TEST_MESSAGE, reply, "serialisation identity via router");

                sendSocket.disconnect("inproc://pair1");
                sendSocket.connect("inproc://pair2");
                TEST_MESSAGE.send(sendSocket);
                final MajordomoProtocol.MdpMessage reply2 = MajordomoProtocol.MdpMessage.receive(receiveSocket2);
                assertEquals(TEST_MESSAGE, reply2, "serialisation identity via dealer");

                MajordomoProtocol.MdpMessage.send(sendSocket, List.of(TEST_MESSAGE));
                final MajordomoProtocol.MdpMessage reply3 = MajordomoProtocol.MdpMessage.receive(receiveSocket2);
                final MajordomoProtocol.MdpMessage clone = new MajordomoProtocol.MdpMessage(TEST_MESSAGE);
                clone.command = MajordomoProtocol.Command.FINAL; // N.B. multiple message exist only for reply type either FINAL, or (PARTIAL, PARTIAL, ..., FINAL)
                assertEquals(clone, reply3, "serialisation identity via dealer");
            }
        }
    }
}
