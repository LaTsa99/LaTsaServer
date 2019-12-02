package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;
import com.latsa.chatserver.utils.CommandFifo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TcpChatServerTest {

    static TcpChatServer tcs;

    @BeforeEach
    void init() {
        tcs = new TcpChatServer(44444, new ServerTerminal(new CommandFifo()));
        Thread t = new Thread(tcs);
        t.start();
        tcs.addUser("Test", "Test");
    }

    @Test
    void getPORT() {
        int port = tcs.getPORT();
        assertEquals(44444, port);

        UserData result = tcs.getUser("Test");
        assertNotNull(result);

        tcs.getPreviousMessages().add("Test");
        tcs.deleteHistory();
        assertEquals(0, tcs.getPreviousMessages().size());

        tcs.banUser("Test", "For testing");
        assertTrue(tcs.getUser("Test").getBanned());

        tcs.removeBan("Test");
        assertFalse(tcs.getUser("Test").getBanned());

        tcs.deleteUser("Test");
        assertNull(tcs.getUser("Test"));

        final String testIp = "192.168.0.1";
        tcs.banIp(testIp);
        assertTrue(tcs.getBlacklist().contains(testIp));

        tcs.unbanIp(testIp);
        assertFalse(tcs.getBlacklist().contains(testIp));
    }

    @AfterAll
    static void destruct()
    {
        tcs.stopServer();
    }
}