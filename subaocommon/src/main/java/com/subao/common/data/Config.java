package com.subao.common.data;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.io.FileOperator;
import com.subao.common.io.Persistent;
import com.subao.common.io.PersistentFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Config {

    private static final Config instance = new Config();

    private static final String FILE_NAME = "config.subao";

    /**
     * 字段名：最近一次上报Start消息是哪一天？
     */
    private static final String NAME_DAY_REPORT_START_MESSAGE = "drsm";

    private final Persistent persistent;

    /**
     * 最近一次上报Start消息是哪一天？
     */
    private int dayReportStartMessage;

    private Config() {
        this.persistent = PersistentFactory.createByFile(FileOperator.getDataFile(FILE_NAME));
        load();
    }

    public static Config getInstance() {
        return instance;
    }

    Persistent getPersistent() {
        return this.persistent;
    }

    boolean load() {
        if (!persistent.exists()) {
            return false;
        }
        boolean result = false;
        JsonReader reader = null;
        try {
            reader = new JsonReader(new BufferedReader(new InputStreamReader(persistent.openInput()), 1024));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (NAME_DAY_REPORT_START_MESSAGE.equals(name)) {
                    dayReportStartMessage = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            result = true;
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            com.subao.common.Misc.close(reader);
        }
        return result;
    }

    /**
     * 返回：最近一次上报Start消息是哪天？
     */
    public int getDayReportStartMessage() {
        return this.dayReportStartMessage;
    }

    /**
     * 设置：最近一次上报Start消息是哪天？
     */
    public void setDayReportStartMessage(int day) {
        if (this.dayReportStartMessage != day) {
            this.dayReportStartMessage = day;
            save();
        }
    }

    private void save() {
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                JsonWriter writer = null;
                try {
                    writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(persistent.openOutput()), 1024));
                    writer.beginObject();
                    writer.name(NAME_DAY_REPORT_START_MESSAGE).value(dayReportStartMessage);
                    writer.endObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } finally {
                    com.subao.common.Misc.close(writer);
                }
            }
        });
    }
}
