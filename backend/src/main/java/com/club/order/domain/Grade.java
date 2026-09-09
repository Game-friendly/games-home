package com.club.order.domain;

/** 等级：A > B > C。rank 越大越高，用于「严格更高等级才能抢」的比较。 */
public enum Grade {
    C(1), B(2), A(3);

    public final int rank;

    Grade(int rank) {
        this.rank = rank;
    }
}
