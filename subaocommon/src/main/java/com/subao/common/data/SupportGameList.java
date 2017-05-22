package com.subao.common.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * {@link SupportGame}的列表，根据{@link AccelGame}列表和本地安装应用列表生成（交集）
 * <p>
 * 此类实例代表本机安装的、且支持的游戏列表
 * <p>Created by YinHaiBo on 2017/3/28.</p>
 */
public class SupportGameList implements Iterable<SupportGame> {

    private final List<SupportGame> list;

    SupportGameList(List<SupportGame> list) {
        this.list = list;
    }

    /**
     * 根据给定的加速游戏列表，与本机安装的应用列表，作交集运算，并返回结果
     *
     * @param accelGameList 加速游戏列表
     * @param installedList 本机安装的APP列表
     * @return 本机已安装的、且被我们支持的游戏列表
     */
    public static SupportGameList build(List<AccelGame> accelGameList, List<InstalledApp.Info> installedList) {
        AccelGameMap map = new AccelGameMap(accelGameList);
        if (accelGameList == null || accelGameList.isEmpty()
            || installedList == null || installedList.isEmpty()) {
            return null;
        }
        List<SupportGame> list = new ArrayList<SupportGame>(16);
        for (InstalledApp.Info info : installedList) {
            String appLabel = info.getAppLabel();
            AccelGame accelGame = map.findAccelGame(info.getPackageName(), appLabel);
            if (accelGame != null) {
                SupportGame supportGame = new SupportGame(
                    info.getUid(), info.getPackageName(), appLabel,
                    accelGame.getProtocol(),
                    accelGame.isForeign(),
                    accelGame.getWhitePorts(),
                    accelGame.getBlackPorts(),
                    accelGame.getWhiteIps(),
                    accelGame.getBlackIps()
                );
                list.add(supportGame);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        return new SupportGameList(list);
    }

    /**
     * 返回列表项个数
     */
    public int getCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public Iterator<SupportGame> iterator() {
        return new IteratorWrapper(list == null ? null : list.iterator());
    }

    /**
     * 返回“本机安装且支持的游戏”的包名列表
     *
     * @return 包名列表，或null
     */
    public List<String> getPackageNameList() {
        int count = this.getCount();
        if (count == 0) {
            return null;
        }
        List<String> list = new ArrayList<String>(count);
        for (SupportGame supportGame : this) {
            list.add(supportGame.packageName);
        }
        return list;
    }

    private static class IteratorWrapper implements Iterator<SupportGame> {

        private final Iterator<SupportGame> iterator;

        private IteratorWrapper(Iterator<SupportGame> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator == null ? false : iterator.hasNext();
        }

        @Override
        public SupportGame next() {
            if (iterator == null) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
