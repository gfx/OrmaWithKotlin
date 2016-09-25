package com.github.gfx.android.orma_kotlin_example

import com.github.gfx.android.orma.annotation.Column
import com.github.gfx.android.orma.annotation.Setter
import com.github.gfx.android.orma.annotation.Table

@Table
data class Location(
        @Setter("latitude") @Column val latitude: Double,
        @Setter("longitude") @Column val longitude: Double) {
    
}
