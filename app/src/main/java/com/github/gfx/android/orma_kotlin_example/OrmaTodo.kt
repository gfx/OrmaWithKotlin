package com.github.gfx.android.orma_kotlin_example

import com.github.gfx.android.orma.annotation.Column
import com.github.gfx.android.orma.annotation.PrimaryKey
import com.github.gfx.android.orma.annotation.Setter
import com.github.gfx.android.orma.annotation.Table
import java.util.*

@Table
data class OrmaTodo(
        @Setter("id") @PrimaryKey var id: Long,
        @Setter("title") @Column(indexed = true) var title: String,
        @Setter("content") @Column var content: String,
        @Setter("done") @Column var done: Boolean,
        @Setter("createdTime") @Column(indexed = true) var createdTime: Date
) {
}
