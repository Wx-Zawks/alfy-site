package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminInquiryResponse;
import com.alfy.api.dto.InquiryCreateRequest;
import com.alfy.api.dto.InquirySubmitResponse;
import com.alfy.api.dto.InquiryUpdateRequest;
import com.alfy.api.entity.Inquiry;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.InquiryMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private static final Set<String> STATUSES = Set.of("NEW", "CONTACTED", "QUALIFIED", "CLOSED", "SPAM");
    private final InquiryMapper inquiryMapper;
    private final AdminOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InquirySubmitResponse submit(InquiryCreateRequest request) {
        if (blank(request.phone()) && blank(request.email())) throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少留下电话或邮箱中的一种联系方式");
        Inquiry inquiry = new Inquiry(); inquiry.setInquiryNo(nextNo()); inquiry.setName(request.name().trim()); inquiry.setCompany(trim(request.company())); inquiry.setPhone(trim(request.phone())); inquiry.setEmail(trim(request.email())); inquiry.setInquiryType(blank(request.inquiryType()) ? "GENERAL" : request.inquiryType().trim()); inquiry.setMessage(trim(request.message())); inquiry.setProductId(request.productId()); inquiry.setSourceUrl(trim(request.sourceUrl())); inquiry.setUtmJson(write(request.utm())); inquiry.setPrivacyAccepted(1); inquiry.setStatus("NEW"); inquiryMapper.insert(inquiry);
        return new InquirySubmitResponse(inquiry.getInquiryNo());
    }

    public Page<AdminInquiryResponse> list(String status, String keyword, long page, long size) {
        if (!blank(status) && !STATUSES.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的询盘状态");
        Page<Inquiry> records = inquiryMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Inquiry>()
                .eq(!blank(status), Inquiry::getStatus, status)
                .and(!blank(keyword), q -> q.like(Inquiry::getName, keyword).or().like(Inquiry::getCompany, keyword).or().like(Inquiry::getPhone, keyword).or().like(Inquiry::getEmail, keyword))
                .orderByDesc(Inquiry::getCreatedAt));
        Page<AdminInquiryResponse> result = new Page<>(records.getCurrent(), records.getSize(), records.getTotal()); result.setRecords(records.getRecords().stream().map(this::toResponse).toList()); return result;
    }
    public AdminInquiryResponse get(Long id) { return toResponse(require(id)); }
    @Transactional
    public AdminInquiryResponse update(Long id, InquiryUpdateRequest request, AdminPrincipal principal) {
        if (!STATUSES.contains(request.status())) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的询盘状态");
        Inquiry inquiry = require(id); inquiry.setStatus(request.status()); inquiry.setAdminNote(trim(request.adminNote())); if (!"NEW".equals(request.status()) && inquiry.getHandledAt() == null) inquiry.setHandledAt(LocalDateTime.now()); inquiryMapper.updateById(inquiry);
        operationLogService.record(principal.id(), "UPDATE", "INQUIRY", id, "更新询盘状态为 " + request.status()); return get(id);
    }
    private Inquiry require(Long id) { Inquiry inquiry = inquiryMapper.selectById(id); if (inquiry == null) throw new BusinessException(ErrorCode.NOT_FOUND, "询盘不存在"); return inquiry; }
    private AdminInquiryResponse toResponse(Inquiry i) { return new AdminInquiryResponse(i.getId(), i.getInquiryNo(), i.getName(), i.getCompany(), i.getPhone(), i.getEmail(), i.getInquiryType(), i.getMessage(), i.getProductId(), i.getSourceUrl(), read(i.getUtmJson()), i.getStatus(), i.getAdminNote(), i.getHandledAt(), i.getCreatedAt()); }
    private String nextNo() { return "AF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ThreadLocalRandom.current().nextInt(100, 1000); }
    private String write(JsonNode node) { if (node == null || node.isNull()) return null; try { return objectMapper.writeValueAsString(node); } catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "推广来源格式不正确"); } }
    private JsonNode read(String json) { if (blank(json)) return null; try { return objectMapper.readTree(json); } catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "推广来源数据无法读取"); } }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
}
