package com.github.gfx.android.orma_kotlin_example

import android.content.Context

object OrmaHolder {
    lateinit var ORMA: OrmaDatabase;

    fun initialize(context: Context) {
        ORMA = OrmaDatabase.builder(context).build();
    }
}
