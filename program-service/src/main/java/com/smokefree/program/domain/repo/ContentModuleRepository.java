package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.ContentModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository để truy cập dữ liệu cho các module nội dung (ContentModule).
 * Đổi tên thành ContentModuleRepo để khớp với service.
 */
public interface ContentModuleRepository extends JpaRepository<ContentModule, UUID> {

    /**
     * Tìm phiên bản mới nhất của một module dựa trên code và lang.
     */
    Optional<ContentModule> findTopByCodeAndLangOrderByVersionDesc(String code, String lang);

    /**
     * Kiểm tra sự tồn tại của một module với code, lang, và version cụ thể.
     */
    boolean existsByCodeAndLangAndVersion(String code, String lang, Integer version);

    /**
     * Tìm tất cả các phiên bản của một module dựa trên code và lang, sắp xếp giảm dần theo version.
     */
    List<ContentModule> findByCodeAndLangOrderByVersionDesc(String code, String lang);

    /**
     * Tìm kiếm module có code chứa một keyword và thuộc một ngôn ngữ nhất định, có hỗ trợ phân trang.
     */
    Page<ContentModule> findByCodeContainingIgnoreCaseAndLang(String codeKeyword, String lang, Pageable pageable);
}
