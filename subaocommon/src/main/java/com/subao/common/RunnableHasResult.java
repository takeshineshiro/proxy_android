package com.subao.common;


/**
 * 一个泛型的Runnable类，用于在两个线程之间执行某些操作并返回结果
 *
 * @param <R> 返回类型
 */
public abstract class RunnableHasResult<R> implements Runnable {

    private boolean condition;

    private R result;

    /**
     * 设置result，并唤醒所有等待值被设定的线程
     */
    protected final void setResult(R r) {
        this.result = r;
        synchronized (this) {
            boolean oldCondition = this.condition;
            this.condition = true;
            if (!oldCondition) {
                this.notifyAll();
            }
        }
    }

    /**
     * 挂起调用者的线程，直到result被成功设置。
     *
     * @return 返回成功设置后的result
     */
    public R waitResult() {
        synchronized (this) {
            while (!condition) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return this.result;
    }

    @Override
    public abstract void run();

}
