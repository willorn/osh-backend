package com.backstage.system.mapper.tool;

import com.backstage.system.domain.tool.OshToolPackage;
import com.backstage.system.domain.vo.tool.ToolQuotaPackageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshToolPackageMapper {

    OshToolPackage selectPackageById(@Param("id") Long id);

    List<OshToolPackage> selectEnabledPackages();

    List<ToolQuotaPackageVO> selectAllPackageVos();

    int insertPackage(OshToolPackage toolPackage);

    int updatePackage(OshToolPackage toolPackage);

    int softDeletePackage(@Param("id") Long id, @Param("operator") String operator);
}
