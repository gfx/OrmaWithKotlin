package com.github.gfx.android.orma_kotlin_example

import com.github.gfx.android.orma.annotation.Column
import com.github.gfx.android.orma.annotation.PrimaryKey
import com.github.gfx.android.orma.annotation.Table

@Table
class User {
    @PrimaryKey(autoincrement = true) var id: Long = 0

    @Column var name: String = ""
}
