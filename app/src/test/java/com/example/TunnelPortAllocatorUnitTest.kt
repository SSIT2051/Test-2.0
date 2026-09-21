package com.example

import com.example.domain.tunnel.LocalPortAllocator
import com.example.domain.tunnel.TunnelProtocol
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunnelPortAllocatorUnitTest {

    @Test
    fun testTcpPortAllocation() {
        val port1 = LocalPortAllocator.allocatePort(TunnelProtocol.TCP, 25565)
        val port2 = LocalPortAllocator.allocatePort(TunnelProtocol.TCP, 25565)
        assertNotEquals("Allocated ports for two servers must not collide", port1, port2)
        assertTrue("Port must be within valid network range", port1 in 1024..65535)
        assertTrue("Port must be within valid network range", port2 in 1024..65535)
    }

    @Test
    fun testUdpPortAllocation() {
        val port1 = LocalPortAllocator.allocatePort(TunnelProtocol.UDP, 19132)
        val port2 = LocalPortAllocator.allocatePort(TunnelProtocol.UDP, 19132)
        assertNotEquals("Allocated Bedrock ports must not collide", port1, port2)
        assertTrue(port1 >= 19132)
        assertTrue(port2 >= 19132)
    }
}
