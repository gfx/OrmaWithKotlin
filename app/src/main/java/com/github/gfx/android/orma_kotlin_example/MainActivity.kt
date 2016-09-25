package com.github.gfx.android.orma_kotlin_example

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.gfx.android.orma.Relation
import com.github.gfx.android.orma.widget.OrmaListAdapter
import com.github.gfx.android.orma_kotlin_example.databinding.ActivityMainBinding
import com.github.gfx.android.orma_kotlin_example.databinding.ItemBinding
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    lateinit var orma: OrmaDatabase

    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        orma = OrmaHolder.ORMA;
        adapter = Adapter(this, orma.relationOfItem().orderByIdAsc())
        binding.list.adapter = adapter

        binding.fab.setOnClickListener {
            adapter.addItemAsObservable({
                Item(0, "content #" + orma.selectFromItem().count())
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "item created!", Toast.LENGTH_SHORT).show()
                    })
        }
    }

    class Adapter(context: Context, relation: Relation<Item, *>) : OrmaListAdapter<Item>(context, relation) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            val binding: ItemBinding
            if (convertView == null) {
                binding = DataBindingUtil.inflate(layoutInflater, R.layout.item, parent, false)
            } else {
                binding = DataBindingUtil.getBinding(convertView)
            }

            val item = getItem(position)
            binding.text.text = item.content;

            binding.root.setOnClickListener {
                removeItemAsObservable(item)
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }

            return binding.root;
        }
    }
}
