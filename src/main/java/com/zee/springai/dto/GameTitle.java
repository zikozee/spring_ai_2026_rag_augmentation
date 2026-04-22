package com.zee.springai.dto;


/**
 * @dev : Ezekiel Eromosei
 * @date : 21 Apr, 2026
 */

public record GameTitle(String title) {

    public String getNormalizedTitle(){
        return title.toLowerCase().replace(" ", "_");
    }

}
