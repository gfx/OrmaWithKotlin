package com.github.gfx.android.orma_kotlin_example


import android.support.annotation.Nullable
import com.github.gfx.android.orma.annotation.Column
import com.github.gfx.android.orma.annotation.PrimaryKey
import com.github.gfx.android.orma.annotation.Setter
import com.github.gfx.android.orma.annotation.Table

@Table
data class DataClass
(
        @Setter("id") @PrimaryKey var id: Long,
        @Setter("todo") @Column @Nullable var todo: OrmaTodo?,
        @Setter("content") @Column @Nullable var content: String?
)
