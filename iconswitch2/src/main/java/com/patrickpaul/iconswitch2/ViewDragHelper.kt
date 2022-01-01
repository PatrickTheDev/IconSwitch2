package com.patrickpaul.iconswitch2

class ViewDragHelper private constructor() {

    init {}

    companion object {
        const val TAG = "ViewDragHelper"
        const val INVALID_POINTER = -1
        private const val EDGE_SIZE = 20 // dp
        private const val BASE_SETTLE_DURATION = 256 // ms
        private const val MAX_SETTLE_DURATION = 600 // ms
    }

}