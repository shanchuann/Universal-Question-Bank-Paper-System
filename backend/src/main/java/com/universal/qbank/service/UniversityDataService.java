package com.universal.qbank.service;

import java.util.*;
import org.springframework.stereotype.Service;

/**
 * 高校数据服务 - 提供中国主流高校及其院系结构数据
 */
@Service
public class UniversityDataService {

  /** 高校信息 */
  public static class University {
    private String code;
    private String name;
    private String province;
    private String city;
    private String type; // 985, 211, 双一流, 普通本科
    private List<Department> departments;

    public University(String code, String name, String province, String city, String type) {
      this.code = code;
      this.name = name;
      this.province = province;
      this.city = city;
      this.type = type;
      this.departments = new ArrayList<>();
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }
  }

  /** 院系信息 */
  public static class Department {
    private String code;
    private String name;

    public Department(String code, String name) {
      this.code = code;
      this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  private static final Map<String, University> UNIVERSITIES = new LinkedHashMap<>();

  static {
    // 北京高校
    University pku = new University("PKU", "北京大学", "北京", "北京", "985");
    pku.getDepartments().addAll(Arrays.asList(
      new Department("PKU_CS", "信息科学技术学院"),
      new Department("PKU_MATH", "数学科学学院"),
      new Department("PKU_PHY", "物理学院"),
      new Department("PKU_CHEM", "化学与分子工程学院"),
      new Department("PKU_LAW", "法学院"),
      new Department("PKU_ECON", "经济学院"),
      new Department("PKU_GSBA", "光华管理学院"),
      new Department("PKU_CHIN", "中国语言文学系"),
      new Department("PKU_HIST", "历史学系"),
      new Department("PKU_PHIL", "哲学系"),
      new Department("PKU_SOC", "社会学系"),
      new Department("PKU_BIO", "生命科学学院"),
      new Department("PKU_ENV", "环境科学与工程学院"),
      new Department("PKU_MED", "医学部")
    ));
    UNIVERSITIES.put(pku.getCode(), pku);

    University thu = new University("THU", "清华大学", "北京", "北京", "985");
    thu.getDepartments().addAll(Arrays.asList(
      new Department("THU_CS", "计算机科学与技术系"),
      new Department("THU_EE", "电子工程系"),
      new Department("THU_AUTO", "自动化系"),
      new Department("THU_SE", "软件学院"),
      new Department("THU_MATH", "数学科学系"),
      new Department("THU_PHY", "物理系"),
      new Department("THU_CHEM", "化学系"),
      new Department("THU_ME", "机械工程系"),
      new Department("THU_CE", "土木工程系"),
      new Department("THU_ARCH", "建筑学院"),
      new Department("THU_ECON", "经济管理学院"),
      new Department("THU_LAW", "法学院"),
      new Department("THU_MED", "医学院"),
      new Department("THU_ART", "美术学院")
    ));
    UNIVERSITIES.put(thu.getCode(), thu);

    University ruc = new University("RUC", "中国人民大学", "北京", "北京", "985");
    ruc.getDepartments().addAll(Arrays.asList(
      new Department("RUC_IS", "信息学院"),
      new Department("RUC_LAW", "法学院"),
      new Department("RUC_ECON", "经济学院"),
      new Department("RUC_BA", "商学院"),
      new Department("RUC_STAT", "统计学院"),
      new Department("RUC_JOUR", "新闻学院"),
      new Department("RUC_LIT", "文学院"),
      new Department("RUC_HIST", "历史学院"),
      new Department("RUC_SOC", "社会与人口学院")
    ));
    UNIVERSITIES.put(ruc.getCode(), ruc);

    University beihang = new University("BUAA", "北京航空航天大学", "北京", "北京", "985");
    beihang.getDepartments().addAll(Arrays.asList(
      new Department("BUAA_CS", "计算机学院"),
      new Department("BUAA_SE", "软件学院"),
      new Department("BUAA_AUTO", "自动化科学与电气工程学院"),
      new Department("BUAA_EE", "电子信息工程学院"),
      new Department("BUAA_AERO", "航空科学与工程学院"),
      new Department("BUAA_ASTRO", "宇航学院"),
      new Department("BUAA_ME", "机械工程及自动化学院"),
      new Department("BUAA_MAT", "材料科学与工程学院"),
      new Department("BUAA_MATH", "数学科学学院")
    ));
    UNIVERSITIES.put(beihang.getCode(), beihang);

    // 上海高校
    University sjtu = new University("SJTU", "上海交通大学", "上海", "上海", "985");
    sjtu.getDepartments().addAll(Arrays.asList(
      new Department("SJTU_CS", "电子信息与电气工程学院"),
      new Department("SJTU_SE", "软件学院"),
      new Department("SJTU_MATH", "数学科学学院"),
      new Department("SJTU_PHY", "物理与天文学院"),
      new Department("SJTU_CHEM", "化学化工学院"),
      new Department("SJTU_ME", "机械与动力工程学院"),
      new Department("SJTU_MAT", "材料科学与工程学院"),
      new Department("SJTU_BA", "安泰经济与管理学院"),
      new Department("SJTU_MED", "医学院"),
      new Department("SJTU_LAW", "凯原法学院")
    ));
    UNIVERSITIES.put(sjtu.getCode(), sjtu);

    University fdu = new University("FDU", "复旦大学", "上海", "上海", "985");
    fdu.getDepartments().addAll(Arrays.asList(
      new Department("FDU_CS", "计算机科学技术学院"),
      new Department("FDU_SE", "软件学院"),
      new Department("FDU_MATH", "数学科学学院"),
      new Department("FDU_PHY", "物理学系"),
      new Department("FDU_CHEM", "化学系"),
      new Department("FDU_BIO", "生命科学学院"),
      new Department("FDU_ECON", "经济学院"),
      new Department("FDU_BA", "管理学院"),
      new Department("FDU_LAW", "法学院"),
      new Department("FDU_JOUR", "新闻学院"),
      new Department("FDU_MED", "上海医学院")
    ));
    UNIVERSITIES.put(fdu.getCode(), fdu);

    University tongji = new University("TJU", "同济大学", "上海", "上海", "985");
    tongji.getDepartments().addAll(Arrays.asList(
      new Department("TJU_CS", "电子与信息工程学院"),
      new Department("TJU_SE", "软件学院"),
      new Department("TJU_ARCH", "建筑与城市规划学院"),
      new Department("TJU_CE", "土木工程学院"),
      new Department("TJU_ME", "机械与能源工程学院"),
      new Department("TJU_AUTO", "汽车学院"),
      new Department("TJU_ENV", "环境科学与工程学院"),
      new Department("TJU_MED", "医学院")
    ));
    UNIVERSITIES.put(tongji.getCode(), tongji);

    // 江苏高校
    University nju = new University("NJU", "南京大学", "江苏", "南京", "985");
    nju.getDepartments().addAll(Arrays.asList(
      new Department("NJU_CS", "计算机科学与技术系"),
      new Department("NJU_SE", "软件学院"),
      new Department("NJU_AI", "人工智能学院"),
      new Department("NJU_MATH", "数学系"),
      new Department("NJU_PHY", "物理学院"),
      new Department("NJU_CHEM", "化学化工学院"),
      new Department("NJU_BIO", "生命科学学院"),
      new Department("NJU_GEO", "地球科学与工程学院"),
      new Department("NJU_BA", "商学院"),
      new Department("NJU_LAW", "法学院")
    ));
    UNIVERSITIES.put(nju.getCode(), nju);

    University seu = new University("SEU", "东南大学", "江苏", "南京", "985");
    seu.getDepartments().addAll(Arrays.asList(
      new Department("SEU_CS", "计算机科学与工程学院"),
      new Department("SEU_SE", "软件学院"),
      new Department("SEU_EE", "信息科学与工程学院"),
      new Department("SEU_ARCH", "建筑学院"),
      new Department("SEU_CE", "土木工程学院"),
      new Department("SEU_ME", "机械工程学院"),
      new Department("SEU_AUTO", "自动化学院"),
      new Department("SEU_BIO", "生物科学与医学工程学院")
    ));
    UNIVERSITIES.put(seu.getCode(), seu);

    // 浙江高校
    University zju = new University("ZJU", "浙江大学", "浙江", "杭州", "985");
    zju.getDepartments().addAll(Arrays.asList(
      new Department("ZJU_CS", "计算机科学与技术学院"),
      new Department("ZJU_SE", "软件学院"),
      new Department("ZJU_EE", "电气工程学院"),
      new Department("ZJU_INFO", "信息与电子工程学院"),
      new Department("ZJU_MATH", "数学科学学院"),
      new Department("ZJU_PHY", "物理学系"),
      new Department("ZJU_CHEM", "化学系"),
      new Department("ZJU_BIO", "生命科学学院"),
      new Department("ZJU_ME", "机械工程学院"),
      new Department("ZJU_MAT", "材料科学与工程学院"),
      new Department("ZJU_BA", "管理学院"),
      new Department("ZJU_MED", "医学院")
    ));
    UNIVERSITIES.put(zju.getCode(), zju);

    // 湖北高校
    University whu = new University("WHU", "武汉大学", "湖北", "武汉", "985");
    whu.getDepartments().addAll(Arrays.asList(
      new Department("WHU_CS", "计算机学院"),
      new Department("WHU_SE", "软件学院"),
      new Department("WHU_CYBER", "国家网络安全学院"),
      new Department("WHU_MATH", "数学与统计学院"),
      new Department("WHU_PHY", "物理科学与技术学院"),
      new Department("WHU_CHEM", "化学与分子科学学院"),
      new Department("WHU_BIO", "生命科学学院"),
      new Department("WHU_LAW", "法学院"),
      new Department("WHU_ECON", "经济与管理学院"),
      new Department("WHU_JOUR", "新闻与传播学院")
    ));
    UNIVERSITIES.put(whu.getCode(), whu);

    University hust = new University("HUST", "华中科技大学", "湖北", "武汉", "985");
    hust.getDepartments().addAll(Arrays.asList(
      new Department("HUST_CS", "计算机科学与技术学院"),
      new Department("HUST_SE", "软件学院"),
      new Department("HUST_EE", "电气与电子工程学院"),
      new Department("HUST_AUTO", "人工智能与自动化学院"),
      new Department("HUST_ME", "机械科学与工程学院"),
      new Department("HUST_MAT", "材料科学与工程学院"),
      new Department("HUST_BIO", "生命科学与技术学院"),
      new Department("HUST_MED", "同济医学院"),
      new Department("HUST_BA", "管理学院")
    ));
    UNIVERSITIES.put(hust.getCode(), hust);

    // 广东高校
    University sysu = new University("SYSU", "中山大学", "广东", "广州", "985");
    sysu.getDepartments().addAll(Arrays.asList(
      new Department("SYSU_CS", "计算机学院"),
      new Department("SYSU_SE", "软件工程学院"),
      new Department("SYSU_DATA", "数据科学与计算机学院"),
      new Department("SYSU_MATH", "数学学院"),
      new Department("SYSU_PHY", "物理学院"),
      new Department("SYSU_CHEM", "化学学院"),
      new Department("SYSU_BIO", "生命科学学院"),
      new Department("SYSU_MED", "中山医学院"),
      new Department("SYSU_BA", "管理学院"),
      new Department("SYSU_LAW", "法学院")
    ));
    UNIVERSITIES.put(sysu.getCode(), sysu);

    University scut = new University("SCUT", "华南理工大学", "广东", "广州", "985");
    scut.getDepartments().addAll(Arrays.asList(
      new Department("SCUT_CS", "计算机科学与工程学院"),
      new Department("SCUT_SE", "软件学院"),
      new Department("SCUT_EE", "电子与信息学院"),
      new Department("SCUT_AUTO", "自动化科学与工程学院"),
      new Department("SCUT_ME", "机械与汽车工程学院"),
      new Department("SCUT_MAT", "材料科学与工程学院"),
      new Department("SCUT_CHEM", "化学与化工学院"),
      new Department("SCUT_ARCH", "建筑学院"),
      new Department("SCUT_BA", "工商管理学院")
    ));
    UNIVERSITIES.put(scut.getCode(), scut);

    // 四川高校
    University scu = new University("SCU", "四川大学", "四川", "成都", "985");
    scu.getDepartments().addAll(Arrays.asList(
      new Department("SCU_CS", "计算机学院"),
      new Department("SCU_SE", "软件学院"),
      new Department("SCU_EE", "电子信息学院"),
      new Department("SCU_MATH", "数学学院"),
      new Department("SCU_PHY", "物理学院"),
      new Department("SCU_CHEM", "化学学院"),
      new Department("SCU_BIO", "生命科学学院"),
      new Department("SCU_MED", "华西医学中心"),
      new Department("SCU_BA", "商学院"),
      new Department("SCU_LAW", "法学院")
    ));
    UNIVERSITIES.put(scu.getCode(), scu);

    University uestc = new University("UESTC", "电子科技大学", "四川", "成都", "985");
    uestc.getDepartments().addAll(Arrays.asList(
      new Department("UESTC_CS", "计算机科学与工程学院"),
      new Department("UESTC_SE", "信息与软件工程学院"),
      new Department("UESTC_EE", "电子科学与工程学院"),
      new Department("UESTC_INFO", "信息与通信工程学院"),
      new Department("UESTC_AUTO", "自动化工程学院"),
      new Department("UESTC_ME", "机械与电气工程学院"),
      new Department("UESTC_OPT", "光电科学与工程学院"),
      new Department("UESTC_MATH", "数学科学学院"),
      new Department("UESTC_PHY", "物理学院")
    ));
    UNIVERSITIES.put(uestc.getCode(), uestc);

    // 陕西高校
    University xjtu = new University("XJTU", "西安交通大学", "陕西", "西安", "985");
    xjtu.getDepartments().addAll(Arrays.asList(
      new Department("XJTU_CS", "计算机科学与技术学院"),
      new Department("XJTU_SE", "软件学院"),
      new Department("XJTU_EE", "电气工程学院"),
      new Department("XJTU_INFO", "电子与信息学部"),
      new Department("XJTU_ME", "机械工程学院"),
      new Department("XJTU_ENERGY", "能源与动力工程学院"),
      new Department("XJTU_MAT", "材料科学与工程学院"),
      new Department("XJTU_MED", "医学部"),
      new Department("XJTU_BA", "管理学院")
    ));
    UNIVERSITIES.put(xjtu.getCode(), xjtu);

    University nwpu = new University("NWPU", "西北工业大学", "陕西", "西安", "985");
    nwpu.getDepartments().addAll(Arrays.asList(
      new Department("NWPU_CS", "计算机学院"),
      new Department("NWPU_SE", "软件学院"),
      new Department("NWPU_AERO", "航空学院"),
      new Department("NWPU_ASTRO", "航天学院"),
      new Department("NWPU_AUTO", "自动化学院"),
      new Department("NWPU_EE", "电子信息学院"),
      new Department("NWPU_ME", "机电学院"),
      new Department("NWPU_MAT", "材料学院")
    ));
    UNIVERSITIES.put(nwpu.getCode(), nwpu);

    // 天津高校
    University nankai = new University("NKU", "南开大学", "天津", "天津", "985");
    nankai.getDepartments().addAll(Arrays.asList(
      new Department("NKU_CS", "计算机学院"),
      new Department("NKU_SE", "软件学院"),
      new Department("NKU_MATH", "数学科学学院"),
      new Department("NKU_PHY", "物理科学学院"),
      new Department("NKU_CHEM", "化学学院"),
      new Department("NKU_BIO", "生命科学学院"),
      new Department("NKU_ECON", "经济学院"),
      new Department("NKU_BA", "商学院"),
      new Department("NKU_LAW", "法学院")
    ));
    UNIVERSITIES.put(nankai.getCode(), nankai);

    University tju = new University("TJUT", "天津大学", "天津", "天津", "985");
    tju.getDepartments().addAll(Arrays.asList(
      new Department("TJUT_CS", "智能与计算学部"),
      new Department("TJUT_SE", "软件学院"),
      new Department("TJUT_EE", "电气自动化与信息工程学院"),
      new Department("TJUT_ME", "机械工程学院"),
      new Department("TJUT_CHEM", "化工学院"),
      new Department("TJUT_CE", "建筑工程学院"),
      new Department("TJUT_ARCH", "建筑学院"),
      new Department("TJUT_MAT", "材料科学与工程学院")
    ));
    UNIVERSITIES.put(tju.getCode(), tju);

    // 山东高校
    University sdu = new University("SDU", "山东大学", "山东", "济南", "985");
    sdu.getDepartments().addAll(Arrays.asList(
      new Department("SDU_CS", "计算机科学与技术学院"),
      new Department("SDU_SE", "软件学院"),
      new Department("SDU_MATH", "数学学院"),
      new Department("SDU_PHY", "物理学院"),
      new Department("SDU_CHEM", "化学与化工学院"),
      new Department("SDU_BIO", "生命科学学院"),
      new Department("SDU_MED", "齐鲁医学院"),
      new Department("SDU_LAW", "法学院"),
      new Department("SDU_ECON", "经济学院")
    ));
    UNIVERSITIES.put(sdu.getCode(), sdu);

    // 黑龙江高校
    University hit = new University("HIT", "哈尔滨工业大学", "黑龙江", "哈尔滨", "985");
    hit.getDepartments().addAll(Arrays.asList(
      new Department("HIT_CS", "计算机科学与技术学院"),
      new Department("HIT_SE", "软件学院"),
      new Department("HIT_EE", "电气工程及自动化学院"),
      new Department("HIT_INFO", "电子与信息工程学院"),
      new Department("HIT_ME", "机电工程学院"),
      new Department("HIT_MAT", "材料科学与工程学院"),
      new Department("HIT_ASTRO", "航天学院"),
      new Department("HIT_CE", "土木工程学院"),
      new Department("HIT_ARCH", "建筑学院")
    ));
    UNIVERSITIES.put(hit.getCode(), hit);

    // 吉林高校
    University jlu = new University("JLU", "吉林大学", "吉林", "长春", "985");
    jlu.getDepartments().addAll(Arrays.asList(
      new Department("JLU_CS", "计算机科学与技术学院"),
      new Department("JLU_SE", "软件学院"),
      new Department("JLU_EE", "电子科学与工程学院"),
      new Department("JLU_INFO", "通信工程学院"),
      new Department("JLU_MATH", "数学学院"),
      new Department("JLU_PHY", "物理学院"),
      new Department("JLU_CHEM", "化学学院"),
      new Department("JLU_BIO", "生命科学学院"),
      new Department("JLU_AUTO", "汽车工程学院"),
      new Department("JLU_MED", "白求恩医学部")
    ));
    UNIVERSITIES.put(jlu.getCode(), jlu);

    // 辽宁高校
    University dlu = new University("DUT", "大连理工大学", "辽宁", "大连", "985");
    dlu.getDepartments().addAll(Arrays.asList(
      new Department("DUT_CS", "计算机科学与技术学院"),
      new Department("DUT_SE", "软件学院"),
      new Department("DUT_EE", "电气工程学院"),
      new Department("DUT_INFO", "信息与通信工程学院"),
      new Department("DUT_ME", "机械工程学院"),
      new Department("DUT_CHEM", "化工学院"),
      new Department("DUT_MAT", "材料科学与工程学院"),
      new Department("DUT_CE", "建设工程学部")
    ));
    UNIVERSITIES.put(dlu.getCode(), dlu);

    // 安徽高校
    University ustc = new University("USTC", "中国科学技术大学", "安徽", "合肥", "985");
    ustc.getDepartments().addAll(Arrays.asList(
      new Department("USTC_CS", "计算机科学与技术学院"),
      new Department("USTC_SE", "软件学院"),
      new Department("USTC_INFO", "信息科学技术学院"),
      new Department("USTC_MATH", "数学科学学院"),
      new Department("USTC_PHY", "物理学院"),
      new Department("USTC_CHEM", "化学与材料科学学院"),
      new Department("USTC_BIO", "生命科学学院"),
      new Department("USTC_ENG", "工程科学学院"),
      new Department("USTC_NANO", "微尺度物质科学国家研究中心")
    ));
    UNIVERSITIES.put(ustc.getCode(), ustc);

    // 福建高校
    University xmu = new University("XMU", "厦门大学", "福建", "厦门", "985");
    xmu.getDepartments().addAll(Arrays.asList(
      new Department("XMU_CS", "信息学院"),
      new Department("XMU_SE", "软件学院"),
      new Department("XMU_MATH", "数学科学学院"),
      new Department("XMU_PHY", "物理科学与技术学院"),
      new Department("XMU_CHEM", "化学化工学院"),
      new Department("XMU_BIO", "生命科学学院"),
      new Department("XMU_ECON", "经济学院"),
      new Department("XMU_BA", "管理学院"),
      new Department("XMU_LAW", "法学院")
    ));
    UNIVERSITIES.put(xmu.getCode(), xmu);

    // 湖南高校
    University csu = new University("CSU", "中南大学", "湖南", "长沙", "985");
    csu.getDepartments().addAll(Arrays.asList(
      new Department("CSU_CS", "计算机学院"),
      new Department("CSU_SE", "软件学院"),
      new Department("CSU_AUTO", "自动化学院"),
      new Department("CSU_INFO", "信息科学与工程学院"),
      new Department("CSU_MAT", "材料科学与工程学院"),
      new Department("CSU_CHEM", "化学化工学院"),
      new Department("CSU_MED", "湘雅医学院"),
      new Department("CSU_BA", "商学院")
    ));
    UNIVERSITIES.put(csu.getCode(), csu);

    University hnu = new University("HNU", "湖南大学", "湖南", "长沙", "985");
    hnu.getDepartments().addAll(Arrays.asList(
      new Department("HNU_CS", "信息科学与工程学院"),
      new Department("HNU_EE", "电气与信息工程学院"),
      new Department("HNU_ME", "机械与运载工程学院"),
      new Department("HNU_CE", "土木工程学院"),
      new Department("HNU_CHEM", "化学化工学院"),
      new Department("HNU_MAT", "材料科学与工程学院"),
      new Department("HNU_ECON", "经济与贸易学院"),
      new Department("HNU_BA", "工商管理学院")
    ));
    UNIVERSITIES.put(hnu.getCode(), hnu);

    // 重庆高校
    University cqu = new University("CQU", "重庆大学", "重庆", "重庆", "985");
    cqu.getDepartments().addAll(Arrays.asList(
      new Department("CQU_CS", "计算机学院"),
      new Department("CQU_SE", "软件学院"),
      new Department("CQU_EE", "电气工程学院"),
      new Department("CQU_AUTO", "自动化学院"),
      new Department("CQU_ME", "机械与运载工程学院"),
      new Department("CQU_CE", "土木工程学院"),
      new Department("CQU_ARCH", "建筑城规学院"),
      new Department("CQU_MAT", "材料科学与工程学院")
    ));
    UNIVERSITIES.put(cqu.getCode(), cqu);

    // 甘肃高校
    University lzu = new University("LZU", "兰州大学", "甘肃", "兰州", "985");
    lzu.getDepartments().addAll(Arrays.asList(
      new Department("LZU_CS", "信息科学与工程学院"),
      new Department("LZU_MATH", "数学与统计学院"),
      new Department("LZU_PHY", "物理科学与技术学院"),
      new Department("LZU_CHEM", "化学化工学院"),
      new Department("LZU_BIO", "生命科学学院"),
      new Department("LZU_GEO", "资源环境学院"),
      new Department("LZU_ATMOS", "大气科学学院"),
      new Department("LZU_MED", "基础医学院")
    ));
    UNIVERSITIES.put(lzu.getCode(), lzu);
  }

  /** 获取所有高校列表 */
  public List<University> getAllUniversities() {
    return new ArrayList<>(UNIVERSITIES.values());
  }

  /** 按省份获取高校列表 */
  public List<University> getUniversitiesByProvince(String province) {
    return UNIVERSITIES.values().stream()
        .filter(u -> u.getProvince().equals(province))
        .toList();
  }

  /** 获取指定高校信息 */
  public University getUniversity(String code) {
    return UNIVERSITIES.get(code);
  }

  /** 获取指定高校的院系列表 */
  public List<Department> getDepartments(String universityCode) {
    University uni = UNIVERSITIES.get(universityCode);
    return uni != null ? uni.getDepartments() : new ArrayList<>();
  }

  /** 获取所有省份列表 */
  public List<String> getAllProvinces() {
    return UNIVERSITIES.values().stream()
        .map(University::getProvince)
        .distinct()
        .sorted()
        .toList();
  }

  /** 搜索高校 */
  public List<University> searchUniversities(String keyword) {
    String lowerKeyword = keyword.toLowerCase();
    return UNIVERSITIES.values().stream()
        .filter(u -> u.getName().toLowerCase().contains(lowerKeyword) 
            || u.getCode().toLowerCase().contains(lowerKeyword)
            || u.getCity().contains(keyword)
            || u.getProvince().contains(keyword))
        .toList();
  }
}
