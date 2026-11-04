package com.example.jolijob.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "job_offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferDocument {

    @Id
    private String id;

    private String companyId;

    @Field(type = FieldType.Text)
    private String companyName;

    @Field(type = FieldType.Text, analyzer = "french")
    private String title;

    @Field(type = FieldType.Text, analyzer = "french")
    private String description;

    @Field(type = FieldType.Keyword)
    private String jobCategory;

    @Field(type = FieldType.Keyword)
    private String specialty;

    @Field(type = FieldType.Keyword)
    private String contractType;

    @Field(type = FieldType.Boolean)
    private boolean remote;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private String region;

    @Field(type = FieldType.Keyword)
    private String department;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Double)
    private BigDecimal salaryMin;

    @Field(type = FieldType.Double)
    private BigDecimal salaryMax;

    @Field(type = FieldType.Boolean)
    private boolean active;

    private String createdAt;
}
