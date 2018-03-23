package com.polidea.rxandroidble2.sample

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import butterknife.internal.Utils.listOf
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

private const val TAG = "DeviceService"

internal class DeviceService : Service() {
    var connectDisposable: Disposable? = null
    var disposables = CompositeDisposable()

    override fun onBind(p0: Intent?): IBinder {
        return object : Binder() {

        }
    }

    private lateinit var rxBleClient: RxBleClient

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        rxBleClient = RxBleClient.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent?.hasExtra("disconnect") == true) {
            disposables.clear()
        } else {
            connect()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "service stopped")
        disposables.dispose()
    }

    fun connect() {
        Log.d(TAG, "connect")
        val macAddress = "5C:31:3E:BF:F7:34"
        connectDisposable?.dispose()
        connectDisposable = rxBleClient.getBleDevice(macAddress)
                .establishConnection(false)
                .flatMap { rxBleConnection ->
                    Single.zip(
                            listOf(
                                    rxBleConnection.read(CHARACTERISTIC_1_UUID, ::updateTemp),
                                    rxBleConnection.read(CHARACTERISTIC_2_UUID, ::updateBatt)
                            ), { raw: Array<Any> -> })
                            .flatMapObservable {
                                // Observable.empty<Any>()
                                Observable.combineLatest(
                                        listOf(
                                                rxBleConnection.setupNotif(CHARACTERISTIC_1_UUID, ::updateTemp),
                                                rxBleConnection.setupNotif(CHARACTERISTIC_2_UUID, ::updateBatt),
                                                rxBleConnection.setupNotif(CHARACTERISTIC_3_UUID, ::updateSolar),
                                                rxBleConnection.setupNotif(CHARACTERISTIC_4_UUID, ::updateUsb),
                                                rxBleConnection.setupNotif(CHARACTERISTIC_5_UUID, ::updateEnergy),
                                                rxBleConnection.setupNotif(CHARACTERISTIC_6_UUID, ::updateGPS)
                                        ), { raw: Array<Any> ->
                                    Log.d(TAG, "received notification")
                                    raw
                                })
                            }
                }
                .subscribe(
                        {
                            Log.d(TAG, "toto")
                        },
                        {
                            Log.e(TAG, "onErrorConnection", it)
                        },
                        {
                            Log.d(TAG, "connection completed")
                        }
                )
        disposables += connectDisposable!!
    }

    private fun updateGPS(bytes: @ParameterName(name = "array") ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun updateEnergy(bytes: @ParameterName(name = "array") ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun updateUsb(bytes: @ParameterName(name = "array") ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun updateSolar(bytes: @ParameterName(name = "array") ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun updateBatt(bytes: ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun updateTemp(bytes: @ParameterName(name = "array") ByteArray) {
        Log.d(TAG, "Update method call.")
    }

    private fun RxBleConnection.read(charUUID: UUID, callback: (array: ByteArray) -> Unit): Single<ByteArray> {
        return readCharacteristic(charUUID).doOnSuccess(callback).onErrorReturn { byteArrayOf() }
    }

    private fun RxBleConnection.setupNotif(charUUID: UUID, callback: (array: ByteArray) -> Unit): Observable<ByteArray> {
        return setupNotification(charUUID).flatMap { Observable.never<ByteArray>() }.doOnNext(callback)
    }

    companion object {
        val CHARACTERISTIC_1_UUID = UUID.fromString("f000aa01-0451-4000-b000-000000000000")
        val CHARACTERISTIC_2_UUID = UUID.fromString("f000aa11-0451-4000-b000-000000000000")
        val CHARACTERISTIC_3_UUID = UUID.fromString("f000aa21-0451-4000-b000-000000000000")
        val CHARACTERISTIC_4_UUID = UUID.fromString("f000aa31-0451-4000-b000-000000000000")
        val CHARACTERISTIC_5_UUID = UUID.fromString("f000aa41-0451-4000-b000-000000000000")
        val CHARACTERISTIC_6_UUID = UUID.fromString("f000aa51-0451-4000-b000-000000000000")
    }
}

private operator fun CompositeDisposable.plusAssign(connectDisposable: Disposable) {
    this.add(connectDisposable)
}
