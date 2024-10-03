package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource


object EspressoUtil {
    private const val resource = "GLOBAL"

    @JvmField
    val counting_id_resource = CountingIdlingResource(resource)
    fun decrement() {
        if (counting_id_resource != null) {
            counting_id_resource.decrement()
        }
    }

    fun incrementNotEmpty() {
        counting_id_resource.increment()
    }

}

inline fun <T> wrapEspressoResource(function: () -> T): T {
    EspressoUtil.incrementNotEmpty()
    return try {
        function()
    } finally {
        EspressoUtil.decrement()
    }
}