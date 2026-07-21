package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminProductCategoryResponse;
import com.alfy.api.dto.AdminProductCategoryUpsertRequest;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCategory;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ProductCategoryMapper;
import com.alfy.api.mapper.ProductMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class AdminProductCategoryService {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private final ProductCategoryMapper mapper; private final ProductMapper productMapper; private final AdminOperationLogService logs;
    public Page<AdminProductCategoryResponse> list(String keyword, long page, long size) { Page<ProductCategory> result = mapper.selectPage(new Page<>(page,size), new LambdaQueryWrapper<ProductCategory>().and(keyword != null && !keyword.isBlank(), q -> q.like(ProductCategory::getName, keyword).or().like(ProductCategory::getSlug, keyword)).orderByAsc(ProductCategory::getSortOrder)); Page<AdminProductCategoryResponse> r = new Page<>(result.getCurrent(),result.getSize(),result.getTotal()); r.setRecords(result.getRecords().stream().map(this::toResponse).toList()); return r; }
    public AdminProductCategoryResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public AdminProductCategoryResponse create(AdminProductCategoryUpsertRequest r, AdminPrincipal p) { validate(r,null); ProductCategory x = new ProductCategory(); apply(x,r); mapper.insert(x); logs.record(p.id(),"CREATE","PRODUCT_CATEGORY",x.getId(),"创建产品分类"); return get(x.getId()); }
    @Transactional public AdminProductCategoryResponse update(Long id, AdminProductCategoryUpsertRequest r, AdminPrincipal p) { ProductCategory x=require(id); version(r.version(),x.getVersion()); validate(r,id); apply(x,r); if(mapper.updateById(x)!=1) throw new BusinessException(ErrorCode.CONFLICT,"产品分类已被其他管理员修改"); logs.record(p.id(),"UPDATE","PRODUCT_CATEGORY",id,"更新产品分类"); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { require(id); if(productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId,id))>0) throw new BusinessException(ErrorCode.BAD_REQUEST,"分类下仍有产品，不能删除"); mapper.deleteById(id); logs.record(p.id(),"DELETE","PRODUCT_CATEGORY",id,"软删除产品分类"); }
    private void validate(AdminProductCategoryUpsertRequest r, Long id) { if(!SLUG.matcher(r.slug().trim()).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST,"slug 仅支持小写字母、数字和连字符"); ProductCategory duplicate=mapper.selectOne(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getSlug,r.slug().trim())); if(duplicate!=null&&!duplicate.getId().equals(id)) throw new BusinessException(ErrorCode.CONFLICT,"slug 已被其他分类使用"); }
    private void apply(ProductCategory x,AdminProductCategoryUpsertRequest r) { x.setName(r.name().trim());x.setSlug(r.slug().trim());x.setSummary(r.summary());x.setCoverMediaId(r.coverMediaId());x.setSortOrder(r.sortOrder()==null?0:r.sortOrder());x.setStatus(Boolean.FALSE.equals(r.enabled())?0:1); }
    private void version(Long wanted,Long actual) { if(wanted==null||!wanted.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT,"产品分类已被其他管理员修改，请刷新后重试"); }
    private ProductCategory require(Long id) { ProductCategory x=mapper.selectById(id);if(x==null)throw new BusinessException(ErrorCode.NOT_FOUND,"产品分类不存在");return x; }
    private AdminProductCategoryResponse toResponse(ProductCategory x){return new AdminProductCategoryResponse(x.getId(),x.getName(),x.getSlug(),x.getSummary(),x.getCoverMediaId(),x.getSortOrder(),Integer.valueOf(1).equals(x.getStatus()),x.getVersion());}
}
