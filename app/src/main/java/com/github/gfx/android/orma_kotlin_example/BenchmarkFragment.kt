package com.github.gfx.android.orma.example.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.gfx.android.orma.AccessThreadConstraint
import com.github.gfx.android.orma.example.handwritten.HandWrittenOpenHelper
import com.github.gfx.android.orma_kotlin_example.OrmaDatabase
import com.github.gfx.android.orma_kotlin_example.OrmaTodo
import com.github.gfx.android.orma_kotlin_example.RealmTodo
import com.github.gfx.android.orma_kotlin_example.databinding.FragmentBenchmarkBinding
import com.github.gfx.android.orma_kotlin_example.databinding.ItemResultBinding
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BenchmarkFragment : Fragment() {

    internal val titlePrefix = "title "

    internal val contentPrefix = "content content content\n" + "content content content\n" + "content content content\n" + " "

    internal lateinit var orma: OrmaDatabase

    internal lateinit var hw: HandWrittenOpenHelper

    internal lateinit var binding: FragmentBenchmarkBinding

    internal lateinit var adapter: ResultAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBenchmarkBinding.inflate(inflater, container, false)

        adapter = ResultAdapter(context)
        binding.list.setAdapter(adapter)

        binding.run.setOnClickListener({ v -> run() })

        return binding.getRoot()
    }

    override fun onResume() {
        super.onResume()

        val realmConf = RealmConfiguration.Builder().build()
        Realm.setDefaultConfiguration(realmConf)
        Realm.deleteRealm(realmConf)

        Schedulers.io().createWorker().schedule {
            context.deleteDatabase("orma-benchmark.db")
            orma = OrmaDatabase.builder(context)
                    .name("orma-benchmark.db")
                    .readOnMainThread(AccessThreadConstraint.NONE)
                    .writeOnMainThread(AccessThreadConstraint.NONE)
                    .trace(false)
                    .build()
            orma.migrate()
        }

        context.deleteDatabase("hand-written.db")
        hw = HandWrittenOpenHelper(context, "hand-written.db")
    }

    override fun onPause() {
        super.onPause()
    }

    internal fun run() {
        Log.d(TAG, "Start performing a set of benchmarks")

        adapter.clear()

        val realm = Realm.getDefaultInstance()
        realm.executeTransaction { r -> r.delete(RealmTodo::class.java) }
        realm.close()

        hw.writableDatabase.execSQL("DELETE FROM todo")

        orma.deleteFromOrmaTodo().executeAsSingle().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).flatMap({ integer -> startInsertWithOrma() }).flatMap({ result ->
            adapter.add(result)
            startInsertWithRealm() // Realm objects can only be accessed on the thread they were created.
        }).flatMap({ result ->
            adapter.add(result)
            startInsertWithHandWritten()
        }).flatMap({ result ->
            adapter.add(result)
            startSelectAllWithOrma()
        }).flatMap({ result ->
            adapter.add(result)
            startSelectAllWithRealm() // Realm objects can only be accessed on the thread they were created.
        }).flatMap({ result ->
            adapter.add(result)
            startSelectAllWithHandWritten()
        }).subscribe(
                { result -> adapter.add(result) },
                { error ->
                    Log.wtf(TAG, error)
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()

                })
    }

    internal fun startInsertWithOrma(): Single<Result> {
        return Single.fromCallable<Result> {
            val result = runWithBenchmark( {
                orma.transactionSync({
                    val now = System.currentTimeMillis()

                    val statement = orma.prepareInsertIntoOrmaTodo()

                    for (i in 0..N_ITEMS - 1) {
                        val todo = OrmaTodo(0, titlePrefix + 1, contentPrefix + 1, false, Date(now))
                        statement.execute(todo)
                    }
                })
            })
            Result("Orma/insert", result)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal fun startInsertWithRealm(): Single<Result> {
        return Single.fromCallable<Result> {
            val result = runWithBenchmark( {
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction { r ->
                    val now = System.currentTimeMillis()

                    for (i in 0..N_ITEMS - 1) {
                        val todo = r.createObject(RealmTodo::class.java)

                        todo.title = titlePrefix + i
                        todo.content = contentPrefix + i
                        todo.createdTime = Date(now)
                    }
                }
                realm.close()
            })
            Result("Realm/insert", result)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal fun startInsertWithHandWritten(): Single<Result> {
        return Single.fromCallable<Result> {
            val result = runWithBenchmark({
                val db = hw.writableDatabase
                db.beginTransaction()

                val inserter = db.compileStatement(
                        "INSERT INTO todo (title, content, done, createdTime) VALUES (?, ?, ?, ?)")

                val now = System.currentTimeMillis()

                for (i in 1..N_ITEMS) {
                    inserter.bindAllArgsAsStrings(arrayOf(titlePrefix + i, // title
                            contentPrefix + i, // content
                            "0", // done
                            now.toString())// createdTime
                    )
                    inserter.executeInsert()
                }

                db.setTransactionSuccessful()
                db.endTransaction()
            })
            Result("HandWritten/insert", result)

        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal fun startSelectAllWithOrma(): Single<Result> {
        return Single.fromCallable<Result> {
            val result = runWithBenchmark({
                val count = AtomicInteger()

                val todos = orma.selectFromOrmaTodo().orderByCreatedTimeAsc()

                for (todo in todos) {
                    @SuppressWarnings("unused")
                    val title = todo.title
                    @SuppressWarnings("unused")
                    val content = todo.content
                    @SuppressWarnings("unused")
                    val createdTime = todo.createdTime

                    count.incrementAndGet()
                }

                if (todos.count() !== count.get()) {
                    throw AssertionError("unexpected get: " + count.get())
                }
                Log.d(TAG, "Orma/forEachAll count: " + count)
            })
            Result("Orma/forEachAll", result)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal fun startSelectAllWithRealm(): Single<Result> {
        return Single.fromCallable {
            val result = runWithBenchmark({
                val count = AtomicInteger()
                val realm = Realm.getDefaultInstance()
                val results = realm.where(RealmTodo::class.java).findAllSorted("createdTime", Sort.ASCENDING)
                for (todo in results) {
                    @SuppressWarnings("unused")
                    val title = todo.title
                    @SuppressWarnings("unused")
                    val content = todo.content
                    @SuppressWarnings("unused")
                    val createdTime = todo.createdTime

                    count.incrementAndGet()
                }
                if (results.size != count.get()) {
                    throw AssertionError("unexpected get: " + count.get())
                }
                realm.close()

                Log.d(TAG, "Realm/forEachAll count: " + count)
            })
            Result("Realm/forEachAll", result)
        }
    }

    internal fun startSelectAllWithHandWritten(): Single<Result> {
        return Single.fromCallable<Result> {
            val result = runWithBenchmark({
                val count = AtomicInteger()

                val db = hw.readableDatabase
                val cursor = db.query(
                        "todo",
                        arrayOf("id, title, content, done, createdTime"),
                        null, null, null, null, "createdTime ASC" // whereClause, whereArgs, groupBy, having, orderBy
                )

                if (cursor.moveToFirst()) {
                    val titleIndex = cursor.getColumnIndexOrThrow("title")
                    val contentIndex = cursor.getColumnIndexOrThrow("content")
                    val createdTimeIndex = cursor.getColumnIndexOrThrow("createdTime")
                    do {
                        @SuppressWarnings("unused")
                        val title = cursor.getString(titleIndex)
                        @SuppressWarnings("unused")
                        val content = cursor.getString(contentIndex)
                        @SuppressWarnings("unused")
                        val createdTime = Date(cursor.getLong(createdTimeIndex))

                        count.incrementAndGet()
                    } while (cursor.moveToNext())
                }
                cursor.close()

                val dbCount = longForQuery(db, "SELECT COUNT(*) FROM todo", null)
                if (dbCount != count.get().toLong()) {
                    throw AssertionError("unexpected get: " + count.get() + " != " + dbCount)
                }

                Log.d(TAG, "HandWritten/forEachAll count: " + count)
            })
            Result("HandWritten/forEachAll", result)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal class Result(val title: String, val elapsedMillis: Long)

    internal class ResultAdapter(context: Context) : ArrayAdapter<Result>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            @SuppressLint("ViewHolder") val binding = ItemResultBinding.inflate(LayoutInflater.from(context), parent, false)

            val result = getItem(position)!!
            binding.title.setText(result.title)
            binding.elapsed.setText("${result.elapsedMillis}ms")

            return binding.getRoot()
        }
    }

    companion object {

        internal val TAG = BenchmarkFragment::class.java.simpleName

        internal val N_ITEMS = 10

        internal val N_OPS = 100

        fun newInstance(): Fragment {
            return BenchmarkFragment()
        }

        internal fun longForQuery(db: SQLiteDatabase, sql: String, args: Array<String>?): Long {
            val cursor = db.rawQuery(sql, args)
            cursor.moveToFirst()
            val value = cursor.getLong(0)
            cursor.close()
            return value
        }

        internal fun runWithBenchmark(task: () -> Unit): Long {
            val t0 = System.nanoTime()

            for (i in 0..N_OPS - 1) {
                task()
            }

            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
        }
    }

}
