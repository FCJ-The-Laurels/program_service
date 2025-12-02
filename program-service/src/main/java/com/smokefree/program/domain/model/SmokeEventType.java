
package com.smokefree.program.domain.model;
public enum SmokeEventType {
    SMOKE,           // Do người dùng báo cáo một lần hút thuốc
    RECOVERY_START,  // (Dự phòng) Bắt đầu làm nhiệm vụ phục hồi
    RECOVERY_SUCCESS,// (Dự phòng) Phục hồi thành công
    RECOVERY_FAIL    // (Dự phòng) Phục hồi thất bại
}

