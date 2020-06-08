package cn.com.heaton.blelibrary.ble.proxy;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.EntityData;

/**
 * Created by LiuLei on 2017/10/30.
 */

public interface RequestLisenter<T> {

    void startScan(BleScanCallback<T> callback, long scanPeriod);

    void stopScan();

    boolean connect(T device, BleConnectCallback<T> callback);

    boolean connect(String address, BleConnectCallback<T> callback);

    void notify(T device, BleNotiftCallback<T> callback);

    void cancelNotify(T device, BleNotiftCallback<T> callback);

    void enableNotify(T device, boolean enable, BleNotiftCallback<T> callback);

    void enableNotifyByUuid(T device, boolean enable, UUID serviceUUID, UUID characteristicUUID, BleNotiftCallback<T> callback);

    void disconnect(T device);

    void disconnect(T device, BleConnectCallback<T> callback);

    boolean read(T device, BleReadCallback<T> callback);

    boolean readByUuid(T device, UUID serviceUUID, UUID characteristicUUID, BleReadCallback<T> callback);

    boolean readRssi(T device, BleReadRssiCallback<T> callback);

    boolean write(T device, byte[] data, BleWriteCallback<T> callback);

    boolean writeByUuid(T device, byte[] data, UUID serviceUUID, UUID characteristicUUID, BleWriteCallback<T> callback);

    boolean writeStrByUuid(T device, String data, UUID serviceUUID, UUID characteristicUUID, BleWriteCallback<T> callback);

    void writeEntity(T device, final byte[] data, int packLength, int delay, BleWriteEntityCallback<T> callback);

    void writeEntity(EntityData entityData, BleWriteEntityCallback<T> callback);

    void cancelWriteEntity();

    boolean setMtu(String address, int mtu, BleMtuCallback<T> callback);

}
