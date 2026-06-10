package com.backstage.system.domain.vo.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * 电子书详情视图对象
 *
 * @author backstage
 */
public class BookDetailVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 电子书ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 封面 */
    private String cover;

    /** 描述 */
    @JsonProperty("desc")
    private String desc;

    /** 试读内容 */
    @JsonProperty("try")
    private String tryContent;

    /** 价格 */
    private String price;

    /** 原价 */
    @JsonProperty("t_price")
    private String tPrice;

    /** 订阅数 */
    @JsonProperty("sub_count")
    private Integer subCount;

    /** 权限等级 */
    private Integer level;

    /** 章节列表 */
    @JsonProperty("book_details")
    private List<BookChapterVO> bookDetails;

    /**
     * 标签
     */
    private List<String> tags;

    /** 是否已购买 */
    private Boolean isbuy;

    /** 是否已收藏 */
    private Boolean isfava;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCover()
    {
        return cover;
    }

    public void setCover(String cover)
    {
        this.cover = cover;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(String desc)
    {
        this.desc = desc;
    }

    public String getTryContent()
    {
        return tryContent;
    }

    public void setTryContent(String tryContent)
    {
        this.tryContent = tryContent;
    }

    public String getPrice()
    {
        return price;
    }

    public void setPrice(String price)
    {
        this.price = price;
    }

    public String getTPrice()
    {
        return tPrice;
    }

    public void setTPrice(String tPrice)
    {
        this.tPrice = tPrice;
    }

    public Integer getSubCount()
    {
        return subCount;
    }

    public void setSubCount(Integer subCount)
    {
        this.subCount = subCount;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public List<BookChapterVO> getBookDetails()
    {
        return bookDetails;
    }

    public void setBookDetails(List<BookChapterVO> bookDetails)
    {
        this.bookDetails = bookDetails;
    }

    public Boolean getIsbuy()
    {
        return isbuy;
    }

    public void setIsbuy(Boolean isbuy)
    {
        this.isbuy = isbuy;
    }

    public Boolean getIsfava() {
        return isfava;
    }

    public void setIsfava(Boolean isfava) {
        this.isfava = isfava;
    }

    public String gettPrice() {
        return tPrice;
    }

    public void settPrice(String tPrice) {
        this.tPrice = tPrice;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

}
