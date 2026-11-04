package com.example.jolijob.search.adapter.out.elasticsearch;

import com.example.jolijob.search.application.JobOffer;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "job_offers", createIndex = false)
public record JobOfferDocument(
        @Id String id,
        String companyId,
        @Field(type = FieldType.Text) String companyName,
        @Field(type = FieldType.Text, analyzer = "french") String title,
        @Field(type = FieldType.Text, analyzer = "french") String description,
        @Field(type = FieldType.Keyword) String jobCategory,
        @Field(type = FieldType.Keyword) String specialty,
        @Field(type = FieldType.Keyword) String contractType,
        @Field(type = FieldType.Boolean) boolean remote,
        @Field(type = FieldType.Keyword) String country,
        @Field(type = FieldType.Keyword) String region,
        @Field(type = FieldType.Keyword) String department,
        @Field(type = FieldType.Keyword) String city,
        @Field(type = FieldType.Double) BigDecimal salaryMin,
        @Field(type = FieldType.Double) BigDecimal salaryMax,
        @Field(type = FieldType.Boolean) boolean active,
        String createdAt
) {
    JobOffer toResult() {
        return new JobOffer(id, companyId, companyName, title, description, jobCategory,
                specialty, contractType, remote, country, region, department, city,
                salaryMin, salaryMax, active, createdAt);
    }
}
