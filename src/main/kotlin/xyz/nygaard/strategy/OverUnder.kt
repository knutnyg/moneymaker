package xyz.nygaard.strategy

import xyz.nygaard.FiriClient

// Legg deg over og under spreaden

interface Strategy {
    fun tick()
}

class OverUnder(firiClient: FiriClient): Strategy {
    override fun tick() {

    }

}