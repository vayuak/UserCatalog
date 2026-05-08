package com.UserCatalogServiceOne.UserCatalog.DTOs;



import lombok.Data;

@Data
public class PostRequest {
    private String mediaId;
    private String caption;
    private Long communityId;
}
