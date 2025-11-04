
package com.smokefree.program.domain.model;
public enum SmokeEventType {
    SMOKE,           // có hút 1 hơi / 1 điếu
    RECOVERY_START,  // bắt đầu flow phục hồi <= 12'
    RECOVERY_SUCCESS,
    RECOVERY_FAIL
}

