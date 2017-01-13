package com.mcc.ocr;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacopobufalino on 01/12/16.
 */

public class OCRecordStorage {
    private static List<OCRecord> storage;

    public static void init() {
        storage = new LinkedList<>();
    }

    public static void eraseStorage() {
        storage.clear();
    }

    ;

    public static void addRecord(OCRecord record) {
        storage.add(record);
    }


    public static List<OCRecord> getStorageContent() {
        return storage;
    }
}
