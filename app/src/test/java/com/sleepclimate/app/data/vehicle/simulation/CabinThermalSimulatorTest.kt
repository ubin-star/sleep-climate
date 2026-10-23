package com.sleepclimate.app.data.vehicle.simulation

import com.sleepclimate.app.data.vehicle.HvacMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinThermalSimulatorTest {

    @Test
    fun `공조가 꺼져 있으면 여름밤 실내 온도가 오른다`() {
        val sim = CabinThermalSimulator(SimulationScenario.SUMMER_NIGHT)
        val start = sim.interiorTemp
        repeat(10) { sim.step() }
        assertTrue(sim.interiorTemp > start)
    }

    @Test
    fun `냉방을 켜면 여름밤에도 실내 온도가 내려간다`() {
        val sim = CabinThermalSimulator(SimulationScenario.SUMMER_NIGHT)
        sim.setMode(HvacMode.COOLING)
        val start = sim.interiorTemp
        repeat(5) { sim.step() }
        assertTrue(sim.interiorTemp < start)
    }

    @Test
    fun `공조 가동 중 배터리가 더 빨리 줄어든다`() {
        val idle = CabinThermalSimulator(SimulationScenario.SUMMER_NIGHT)
        val cooling = CabinThermalSimulator(SimulationScenario.SUMMER_NIGHT).apply { setMode(HvacMode.COOLING) }
        repeat(10) { idle.step(); cooling.step() }
        assertTrue(cooling.batteryPercent < idle.batteryPercent)
    }

    @Test
    fun `reset은 시나리오 초기 상태로 되돌리고 공조를 끈다`() {
        val sim = CabinThermalSimulator(SimulationScenario.SUMMER_NIGHT)
        sim.setMode(HvacMode.COOLING)
        repeat(3) { sim.step() }
        sim.reset(SimulationScenario.WINTER_NIGHT)
        assertEquals(SimulationScenario.WINTER_NIGHT.initialInteriorCelsius, sim.interiorTemp, 0.001f)
        assertEquals(HvacMode.OFF, sim.hvacMode)
    }
}
