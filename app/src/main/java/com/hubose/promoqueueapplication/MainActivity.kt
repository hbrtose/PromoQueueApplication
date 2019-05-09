package com.hubose.promoqueueapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {

    val taskQueue: PublishSubject<Runnable> = PublishSubject.create()
    val queueState: PublishSubject<Boolean> = PublishSubject.create()
    val stateText: MutableLiveData<String> = MutableLiveData()
    var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        stateText.observe(this, Observer { tv_state.text = it })

        disposable.add(taskQueue
            .observeOn(Schedulers.single())
            .map { mapTaskToId(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
            {
                stateText.value = "Task $it finished"
                queueState.onNext(false)
            },{
                stateText.value = it.message
            }
        ))

        disposable.add(queueState
            .subscribeOn(Schedulers.io())
            .throttleFirst(10, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if(!it){
                    stateText.value = "Finished"
                }
            }
        )

        button_add.setOnClickListener {
            taskQueue.onNext(Runnable {
                Thread.sleep(et_length.text.toString().toLong()*1000)
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    private fun mapTaskToId(r: Runnable): Int {
        queueState.onNext(true)
        r.run()
        return r.hashCode()
    }
}
