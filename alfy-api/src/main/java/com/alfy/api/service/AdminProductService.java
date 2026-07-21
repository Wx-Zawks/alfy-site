package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminProductResponse;
import com.alfy.api.dto.AdminProductUpsertRequest;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCategory;
import com.alfy.api.entity.ProductSceneRel;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.mapper.ProductCategoryMapper;
import com.alfy.api.mapper.ProductMapper;
import com.alfy.api.mapper.ProductSceneRelMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class AdminProductService {
    private static final Pattern SLUG=Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*"); private static final Set<String> STATUS=Set.of("DRAFT","PUBLISHED","OFFLINE");
    private final ProductMapper mapper; private final ProductCategoryMapper categoryMapper; private final ApplicationSceneMapper sceneMapper; private final ProductSceneRelMapper relationMapper; private final AdminOperationLogService logs; private final HtmlSanitizer sanitizer; private final ObjectMapper json;
    public Page<AdminProductResponse> list(String status, Long categoryId, String keyword,long page,long size){if(status!=null&&!status.isBlank()&&!STATUS.contains(status))throw new BusinessException(ErrorCode.BAD_REQUEST,"不支持的产品状态"); Page<Product> result=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<Product>().eq(status!=null&&!status.isBlank(),Product::getStatus,status).eq(categoryId!=null,Product::getCategoryId,categoryId).and(keyword!=null&&!keyword.isBlank(),q->q.like(Product::getName,keyword).or().like(Product::getSlug,keyword)).orderByDesc(Product::getIsFeatured).orderByAsc(Product::getSortOrder));Page<AdminProductResponse> out=new Page<>(result.getCurrent(),result.getSize(),result.getTotal());out.setRecords(result.getRecords().stream().map(this::toResponse).toList());return out;}
    public AdminProductResponse get(Long id){return toResponse(require(id));}
    @Transactional public AdminProductResponse create(AdminProductUpsertRequest r,AdminPrincipal p){validate(r,null);Product x=new Product();apply(x,r);x.setStatus("DRAFT");mapper.insert(x);replaceScenes(x.getId(),r.sceneIds());logs.record(p.id(),"CREATE","PRODUCT",x.getId(),"创建产品草稿");return get(x.getId());}
    @Transactional public AdminProductResponse update(Long id,AdminProductUpsertRequest r,AdminPrincipal p){Product x=require(id);version(r.version(),x.getVersion());validate(r,id);apply(x,r);if(mapper.updateById(x)!=1)throw new BusinessException(ErrorCode.CONFLICT,"产品已被其他管理员修改");replaceScenes(id,r.sceneIds());logs.record(p.id(),"UPDATE","PRODUCT",id,"更新产品");return get(id);}
    @Transactional public AdminProductResponse publish(Long id,AdminPrincipal p){Product x=require(id);ProductCategory category=categoryMapper.selectById(x.getCategoryId());if(x.getName()==null||x.getName().isBlank()||x.getSlug()==null||x.getSlug().isBlank()||x.getSummary()==null||x.getSummary().isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"发布前必须填写名称、slug 和摘要");if(category==null||!Integer.valueOf(1).equals(category.getStatus()))throw new BusinessException(ErrorCode.BAD_REQUEST,"发布产品前，所属分类必须启用");x.setStatus("PUBLISHED");if(x.getPublishedAt()==null)x.setPublishedAt(LocalDateTime.now());mapper.updateById(x);logs.record(p.id(),"PUBLISH","PRODUCT",id,"发布产品");return get(id);}
    @Transactional public AdminProductResponse offline(Long id,AdminPrincipal p){Product x=require(id);x.setStatus("OFFLINE");mapper.updateById(x);logs.record(p.id(),"OFFLINE","PRODUCT",id,"下线产品");return get(id);}
    @Transactional public void delete(Long id,AdminPrincipal p){require(id);mapper.deleteById(id);relationMapper.delete(new LambdaQueryWrapper<ProductSceneRel>().eq(ProductSceneRel::getProductId,id));logs.record(p.id(),"DELETE","PRODUCT",id,"软删除产品");}
    private void validate(AdminProductUpsertRequest r,Long current){if(!SLUG.matcher(r.slug().trim()).matches())throw new BusinessException(ErrorCode.BAD_REQUEST,"slug 仅支持小写字母、数字和连字符");if(categoryMapper.selectById(r.categoryId())==null)throw new BusinessException(ErrorCode.BAD_REQUEST,"产品分类不存在");Product other=mapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getSlug,r.slug().trim()));if(other!=null&&!other.getId().equals(current))throw new BusinessException(ErrorCode.CONFLICT,"slug 已被其他产品使用");Set<Long> ids=r.sceneIds()==null?Set.of():new LinkedHashSet<>(r.sceneIds());if(r.sceneIds()!=null&&(ids.size()!=r.sceneIds().size()||sceneMapper.selectCount(new LambdaQueryWrapper<ApplicationScene>().in(ApplicationScene::getId,ids))!=ids.size()))throw new BusinessException(ErrorCode.BAD_REQUEST,"关联场景不存在或重复");}
    private void apply(Product x,AdminProductUpsertRequest r){x.setCategoryId(r.categoryId());x.setName(r.name().trim());x.setSlug(r.slug().trim());x.setSummary(r.summary());String html=sanitizer.clean(r.contentHtml());x.setContentHtml(html);x.setContentText(r.contentText()==null?sanitizer.toPlainText(html):r.contentText());x.setCoverMediaId(r.coverMediaId());try{x.setFeaturesJson(json.writeValueAsString(r.features()==null?List.of():r.features()));x.setSpecificationsJson(r.specifications()==null?null:json.writeValueAsString(r.specifications()));}catch(Exception e){throw new BusinessException(ErrorCode.BAD_REQUEST,"产品结构化字段格式错误");}x.setIsFeatured(Boolean.TRUE.equals(r.featured())?1:0);x.setSortOrder(r.sortOrder()==null?0:r.sortOrder());x.setSeoTitle(r.seoTitle());x.setSeoDescription(r.seoDescription());x.setSeoKeywords(r.seoKeywords());}
    private void replaceScenes(Long productId,List<Long> ids){relationMapper.delete(new LambdaQueryWrapper<ProductSceneRel>().eq(ProductSceneRel::getProductId,productId));if(ids==null)return;int order=0;for(Long id:new LinkedHashSet<>(ids)){ProductSceneRel r=new ProductSceneRel();r.setProductId(productId);r.setSceneId(id);r.setSortOrder(order++);relationMapper.insert(r);}}
    private List<Long> scenes(Long id){return relationMapper.selectList(new LambdaQueryWrapper<ProductSceneRel>().eq(ProductSceneRel::getProductId,id).orderByAsc(ProductSceneRel::getSortOrder)).stream().map(ProductSceneRel::getSceneId).collect(Collectors.toList());}
    private List<String> features(String value){try{return value==null?List.of():json.readValue(value,new TypeReference<List<String>>(){});}catch(Exception e){return List.of();}}
    private void version(Long wanted,Long actual){if(wanted==null||!wanted.equals(actual))throw new BusinessException(ErrorCode.CONFLICT,"产品已被其他管理员修改，请刷新后重试");} private Product require(Long id){Product x=mapper.selectById(id);if(x==null)throw new BusinessException(ErrorCode.NOT_FOUND,"产品不存在");return x;}
    private AdminProductResponse toResponse(Product x){JsonNode spec=null;try{if(x.getSpecificationsJson()!=null)spec=json.readTree(x.getSpecificationsJson());}catch(Exception ignored){}return new AdminProductResponse(x.getId(),x.getCategoryId(),x.getName(),x.getSlug(),x.getSummary(),x.getContentHtml(),x.getContentText(),x.getCoverMediaId(),features(x.getFeaturesJson()),spec,Integer.valueOf(1).equals(x.getIsFeatured()),x.getSortOrder(),x.getStatus(),x.getSeoTitle(),x.getSeoDescription(),x.getSeoKeywords(),x.getPublishedAt(),x.getVersion(),scenes(x.getId()));}
}
