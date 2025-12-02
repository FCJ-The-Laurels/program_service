package com.smokefree.program.web.dto.program;

import java.util.UUID;

public record CreateProgramReq(
        // Old field: planDays (giữ lại để backward compat nếu cần, hoặc xóa nếu FE đồng ý)
        // Nhưng theo yêu cầu, ta sẽ ưu tiên planTemplateId
        Integer planDays, 
        
        UUID planTemplateId, // Bắt buộc
        Boolean trial,       // Optional, default true
        UUID coachId
) {}