package com.beyond.ordersystem.product.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdatStockDto;
import com.beyond.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Autowired
    public ProductService(ProductRepository productRepository, StockInventoryService stockInventoryService,S3Client s3Client) {
        this.productRepository = productRepository;
        this.stockInventoryService = stockInventoryService;
        this.s3Client = s3Client;
    }

    // create
    public Product productCreate(ProductSaveReqDto dto){
        MultipartFile image = dto.getProductImage();
        Product product = null;
        try{
            product=productRepository.save(ProductSaveReqDto.toEntity(dto));

            byte[] bytes =image.getBytes();
            Path path = Paths.get("C:/Users/erp/Desktop/tmp", product.getId()+"_"+ image.getOriginalFilename());
            Files.write(path,bytes, StandardOpenOption.CREATE,StandardOpenOption.WRITE); //s3로 바꾸면 큰 의미 없어질 것
            product.updateImagePath(path.toString()); // jpa의 더티체킹 - 변경감지로 인해 update가 가능함


            // --- Redis 적용
            if(dto.getName().contains("sales")){
                stockInventoryService.increaseStock(product.getId(), dto.getStockQuantity());
            }
        }
        catch (IOException e){
            // checked 이다.. 시스템 작업은 checked이다... unchecked랑 checked 알아아햐
            // 전역적인 핸들러가 잡아줘야함
            // 트랜잭션 처리를 위해 일부러 예외를 던지는 것
            e.printStackTrace();
            throw new RuntimeException("이미지 저장 실패");

        }
        return product;
//        return ProductResDto.FromEntity(dto.toEntity());

    }

    //list
    public Page<ProductResDto> productList(ProductSearchDto searchDto, Pageable pageable){
       // 검색을 위해 Specification 객체 사용
        // Specification객체는 복잡한 쿼리를 명세를 이용하여 정의하는 방식으로, 쿼리를 쉽게 생성
        // if(category.isPrsent) && if(searchName.isPrsent) select * from product wherer category like '변수'~
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if(searchDto.getSearchName() !=null){
                    // root: 엔티티의 속성을 접근하기 위한 객체,
                    // CriteriaBuilder는 쿼리를 생성하기 위한 객체
                    predicates.add(criteriaBuilder.like(root.get("name"), "%"+searchDto.getSearchName()+"%"));
                }
                if(searchDto.getCategory()!=null){
                    predicates.add(criteriaBuilder.like(root.get("category"), "%"+searchDto.getCategory()+"%"));

                }
                Predicate[] predicateArr = new Predicate[predicates.size()];
                for(int i=0;i<predicateArr.length;i++){
                    predicateArr[i]=predicates.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        Page<Product> products = productRepository.findAll(specification,pageable);
//        Page<ProductResDto> productResDtos = products.map(a->ProductResDto.FromEntity(a));
        return products.map(ProductResDto::FromEntity);
    }

    public Product createAwsProduct(ProductSaveReqDto createProductRequest) {
        MultipartFile image = createProductRequest.getProductImage();
        Product product = null;

        try {

            product = ProductSaveReqDto.toEntity(createProductRequest);
            Product savedProduct = productRepository.save(product);

            byte[] bytes = image.getBytes(); // 이미지 자체는 바이트 형태로 받아옴. 근데 바이트 형태로는 아마 AWS에 올라가지 않을 것이다.
            String fileName = product.getId() + "_" + image.getOriginalFilename();
            // 로컬 PC에 임시저장. 파일로 바꾸고 올리기 위해서!!
            // 프론트엔드에서 직접 올릴 때에는 이렇게 변환해줄 필요가 없다. 파일로 가지고 있으니까
            Path path = Paths.get("C:\\Users\\erp\\Desktop\\tmp", fileName);
            // local pc에 임시 저장
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            // aws에 pc에 저장된 파일을 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();


            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromFile(path));
            System.out.println( "putobjectresponse"+putObjectResponse);
            // 이 s3Path는 https:// 를 달고 나오는 파일이다.
            String s3Path = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            System.out.println("s3path"+s3Path);
            savedProduct.updateImagePath(s3Path);
            System.out.println("project ID"+savedProduct.getId());
            return savedProduct;
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패");
        }
    }

    public ProductResDto productDetail(Long id){
        Product product = productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당 상품이 없습니다."));
        return ProductResDto.FromEntity(product);
    }

    public Product productUpdateStock(ProductUpdatStockDto dto){
        Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("프로덕트 없습니다"));
        product.updateStockQuantity(dto.getProductQuantity());
        return product;
    }
}
