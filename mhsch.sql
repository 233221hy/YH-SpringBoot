-- MySQL dump 10.13  Distrib 5.7.44, for Win64 (x86_64)
--
-- Host: nj-cdb-0j6ax1c9.sql.tencentcdb.com    Database: mhsch_2
-- ------------------------------------------------------
-- Server version	5.7.36-txsql-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- Table structure for table `yee_announcement`
--

DROP TABLE IF EXISTS `yee_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_announcement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL COMMENT '公告标题',
  `content` text COMMENT '内容',
  `addTime` datetime DEFAULT NULL COMMENT '时间',
  `courseId` int(11) DEFAULT '0' COMMENT '课程',
  `userId` int(11) DEFAULT '0' COMMENT '发布人',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1005689 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='课程公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_announcement_informed`
--

DROP TABLE IF EXISTS `yee_announcement_informed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_announcement_informed` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程ID',
  `userId` int(11) DEFAULT NULL COMMENT '接受人',
  `announcementId` int(11) DEFAULT NULL COMMENT '公告ID',
  `schoolId` int(11) NOT NULL DEFAULT '0' COMMENT '学校Id',
  `readCount` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COMMENT='已收到公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_basket`
--

DROP TABLE IF EXISTS `yee_basket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_basket` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '账号Id',
  `type` int(11) DEFAULT NULL COMMENT '题型',
  `exId` int(11) DEFAULT NULL COMMENT '题目id',
  `score` int(11) DEFAULT NULL COMMENT '分值',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `remote` tinyint(4) DEFAULT '0' COMMENT '是否名华远程试题',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=166263 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='试题篮筐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_category`
--

DROP TABLE IF EXISTS `yee_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '分类名称',
  `allow` tinyint(1) DEFAULT '0' COMMENT '审核',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `pid` int(11) DEFAULT NULL COMMENT '所属分类',
  `code` varchar(100) DEFAULT NULL COMMENT '分类代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1574 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='学科分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_chapter`
--

DROP TABLE IF EXISTS `yee_chapter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_chapter` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '章节名称',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `sort` int(11) DEFAULT NULL COMMENT '排序',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`schoolId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1092443 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='章节';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_class_online`
--

DROP TABLE IF EXISTS `yee_class_online`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_class_online` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL,
  `classId` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `online` int(255) DEFAULT NULL,
  `schoolId` int(11) DEFAULT NULL,
  `save` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `classDate` (`classId`,`date`) USING BTREE,
  KEY `courseDate` (`courseId`,`date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=34590 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_classes`
--

DROP TABLE IF EXISTS `yee_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_classes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '班级名称',
  `collegeId` int(11) DEFAULT NULL COMMENT '所属学院',
  `allow` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  `entryYear` int(4) NOT NULL COMMENT '学生年级',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schoolId` (`schoolId`,`collegeId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1007472 DEFAULT CHARSET=utf8mb4 COMMENT='班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_collection_topic`
--

DROP TABLE IF EXISTS `yee_collection_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_collection_topic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT '0' COMMENT '用户Id',
  `topicId` int(11) DEFAULT '0' COMMENT '试题Id',
  `workId` int(11) DEFAULT '0' COMMENT '作业ID',
  `addTime` datetime DEFAULT NULL COMMENT '添加时间',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29468 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='收藏题目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_college`
--

DROP TABLE IF EXISTS `yee_college`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_college` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '学院名称',
  `allow` tinyint(1) DEFAULT '0' COMMENT '审核',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1000146 DEFAULT CHARSET=utf8mb4 COMMENT='学院';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course`
--

DROP TABLE IF EXISTS `yee_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '课程名称',
  `mode` int(11) DEFAULT NULL COMMENT '课程模式',
  `collegeId` int(11) DEFAULT NULL COMMENT '所属学院',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `lecturers` json DEFAULT NULL COMMENT '指派主讲老师',
  `startDate` date DEFAULT NULL COMMENT '开课时间',
  `endDate` date DEFAULT NULL COMMENT '结束时间',
  `cover` varchar(200) DEFAULT NULL COMMENT '课程封面图',
  `content` text COMMENT '课程介绍',
  `credit` decimal(18,2) DEFAULT NULL COMMENT '学分',
  `allow` tinyint(1) DEFAULT NULL COMMENT '是否发布',
  `intro` text CHARACTER SET utf8 COMMENT '课程简介',
  `teacherIntro` text COMMENT '教师简介',
  `code` varchar(100) DEFAULT NULL COMMENT '课程代码',
  `stuCount` int(11) DEFAULT '0' COMMENT '人数',
  `proclamation` text CHARACTER SET utf8 COMMENT '公告',
  `clusterId` int(11) DEFAULT '0' COMMENT '所属簇',
  `periodName` varchar(100) DEFAULT NULL COMMENT '期数名称',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `createId` int(11) DEFAULT NULL COMMENT '创建者',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  `signStartTime` datetime DEFAULT NULL COMMENT '报名开始时间',
  `signEndTime` datetime DEFAULT NULL COMMENT '报名结束时间',
  `signScope` int(11) DEFAULT NULL COMMENT '报名范围',
  `signClass` json DEFAULT NULL COMMENT '选择班级',
  `lecturerName` varchar(200) DEFAULT NULL COMMENT '主讲老师',
  `offline` tinyint(1) DEFAULT '0' COMMENT '离线教学',
  `mission` tinyint(1) DEFAULT '0' COMMENT '关卡',
  `signLimit` int(11) DEFAULT '0' COMMENT '报名人数上限',
  `lineLock` tinyint(1) DEFAULT NULL COMMENT 'lineLock',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  `tplId` int(11) DEFAULT '0' COMMENT '模板Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `startDate` (`startDate`),
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1011409 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_class`
--

DROP TABLE IF EXISTS `yee_course_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_class` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '教学班级名称',
  `courseId` int(11) NOT NULL COMMENT '课程Id',
  `teacherId` int(11) DEFAULT NULL COMMENT '责任老师',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `allow` tinyint(1) DEFAULT '0' COMMENT '是否审核',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `createId` int(11) DEFAULT NULL COMMENT '创建人',
  `change` tinyint(1) DEFAULT NULL COMMENT '有更新',
  `calculate` tinyint(1) DEFAULT '0' COMMENT '可以计算成绩',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`teacherId`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1022628 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_cluster`
--

DROP TABLE IF EXISTS `yee_course_cluster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '课程名称',
  `versions` int(11) DEFAULT NULL COMMENT '版本数量',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1010823 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程簇';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_evaluation`
--

DROP TABLE IF EXISTS `yee_course_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_evaluation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT '0' COMMENT '课程id',
  `userId` int(11) DEFAULT '0' COMMENT '用户ID',
  `score` int(11) DEFAULT '0' COMMENT '得分',
  `addTime` datetime DEFAULT NULL COMMENT '打分时间',
  `platform` varchar(100) DEFAULT NULL COMMENT '平台',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `couseId` (`courseId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1093931 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='课程评价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_files`
--

DROP TABLE IF EXISTS `yee_course_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '文件名称',
  `uploadPath` varchar(300) DEFAULT NULL COMMENT '上传路径',
  `timeView` int(11) DEFAULT NULL COMMENT '查看次数',
  `createUserId` int(11) DEFAULT NULL COMMENT '上传人',
  `addTime` datetime DEFAULT NULL COMMENT '上传时间',
  `fileName` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '文件名称',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1009944 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='资料下载';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_live`
--

DROP TABLE IF EXISTS `yee_course_live`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_live` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `link` varchar(100) DEFAULT NULL COMMENT '直播链接',
  `pwd` varchar(100) DEFAULT NULL COMMENT '房间密码',
  `schoolId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程直播间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_point_days`
--

DROP TABLE IF EXISTS `yee_course_point_days`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_point_days` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) NOT NULL DEFAULT '0',
  `date` date NOT NULL,
  `studentId` int(11) NOT NULL DEFAULT '0',
  `point` int(11) NOT NULL DEFAULT '0',
  `rank` int(11) NOT NULL DEFAULT '0',
  `point2` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `student` (`courseId`,`date`,`studentId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_point_main`
--

DROP TABLE IF EXISTS `yee_course_point_main`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_point_main` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) NOT NULL DEFAULT '0',
  `studentId` int(11) NOT NULL DEFAULT '0',
  `point` int(11) NOT NULL DEFAULT '0',
  `rank` int(11) NOT NULL DEFAULT '0',
  `point2` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `student` (`courseId`,`studentId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_point_month`
--

DROP TABLE IF EXISTS `yee_course_point_month`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_point_month` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) NOT NULL DEFAULT '0',
  `month` date NOT NULL,
  `studentId` int(11) NOT NULL DEFAULT '0',
  `point` int(11) NOT NULL DEFAULT '0',
  `rank` int(11) NOT NULL DEFAULT '0',
  `point2` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `student` (`courseId`,`month`,`studentId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_points`
--

DROP TABLE IF EXISTS `yee_course_points`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_points` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户',
  `evType` int(11) DEFAULT NULL COMMENT '事件类型',
  `score` int(11) DEFAULT NULL COMMENT '得分',
  `schoolId` int(11) DEFAULT NULL COMMENT '学校Id',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `addTimes` int(11) DEFAULT '0' COMMENT '添加时间',
  `addTime` datetime GENERATED ALWAYS AS (date_format(from_unixtime(`addTimes`),_utf8mb4'%Y-%m-%d %H:%i:%s')) VIRTUAL,
  `addDate` date GENERATED ALWAYS AS (date_format(from_unixtime(`addTimes`),_utf8mb4'%Y-%m-%d')) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseTime` (`courseId`,`addDate`,`userId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_points_lock`
--

DROP TABLE IF EXISTS `yee_course_points_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_points_lock` (
  `id` int(11) NOT NULL DEFAULT '0',
  `lock` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_results`
--

DROP TABLE IF EXISTS `yee_course_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `score` decimal(18,2) DEFAULT NULL COMMENT '成绩得分',
  `videoScore` decimal(18,2) DEFAULT NULL COMMENT '视频得分',
  `examScore` decimal(18,2) DEFAULT NULL COMMENT '测试得分',
  `workScore` decimal(18,2) DEFAULT NULL COMMENT '作业得分',
  `discussScore` decimal(18,2) DEFAULT NULL COMMENT '讨论得分',
  `extraScore` decimal(18,2) DEFAULT NULL COMMENT '额外得分',
  `stuName` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '学生姓名',
  `stuNumber` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '学号',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `ranking` int(11) DEFAULT NULL COMMENT '排名',
  `videoResult` decimal(18,2) DEFAULT NULL COMMENT '视频成绩',
  `examResult` decimal(18,2) DEFAULT NULL COMMENT '测试成绩',
  `workResult` decimal(18,2) DEFAULT NULL COMMENT '作业成绩',
  `discussResult` decimal(18,2) DEFAULT NULL COMMENT '讨论成绩',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `calcDate` date DEFAULT NULL COMMENT '计算时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`) USING BTREE,
  KEY `courseId_2` (`courseId`,`classId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2191722 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程成绩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_score_rules`
--

DROP TABLE IF EXISTS `yee_course_score_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_score_rules` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `useVideo` tinyint(1) DEFAULT NULL COMMENT '使用视频',
  `videoRatio` int(11) DEFAULT NULL COMMENT '视频比例',
  `useDiscuss` tinyint(1) DEFAULT NULL COMMENT '讨论',
  `discussRatio` int(11) DEFAULT NULL COMMENT '讨论',
  `discussItems` json DEFAULT NULL COMMENT '讨论',
  `useWork` tinyint(1) DEFAULT NULL COMMENT '作业',
  `workRatio` int(11) DEFAULT NULL COMMENT '作业',
  `workItems` json DEFAULT NULL COMMENT '作业计分',
  `useExam` tinyint(1) DEFAULT NULL COMMENT '考试',
  `examRatio` int(11) DEFAULT NULL COMMENT '考试比例',
  `examItems` json DEFAULT NULL COMMENT '作业计分',
  `useExtra` tinyint(1) DEFAULT NULL COMMENT '额外',
  `extraRatio` int(11) DEFAULT NULL COMMENT '额外比例',
  `addTime` datetime DEFAULT NULL COMMENT '添加时间',
  `calcNumber` int(11) DEFAULT '0' COMMENT '计算次数',
  `updateTime` datetime DEFAULT NULL COMMENT '更新时间',
  `videoItems` json DEFAULT NULL COMMENT '视频计分',
  `realTime` tinyint(1) DEFAULT '0' COMMENT '实时',
  `videoMode` int(11) DEFAULT '0' COMMENT '视频分配模式',
  `description` text COMMENT '规则说明',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `announce` tinyint(1) DEFAULT '0' COMMENT '公布成绩',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1018843 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程计分规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_sign_class`
--

DROP TABLE IF EXISTS `yee_course_sign_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_sign_class` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `classId` int(11) DEFAULT '0' COMMENT '班级Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程报名班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_sign_student`
--

DROP TABLE IF EXISTS `yee_course_sign_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_sign_student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `studentId` int(11) DEFAULT '0' COMMENT '学生Id',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `signTime` datetime DEFAULT NULL COMMENT '报名时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `studentId` (`studentId`),
  KEY `courseId` (`courseId`,`schoolId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=168349 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程报名学生';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_student`
--

DROP TABLE IF EXISTS `yee_course_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `classId` int(11) DEFAULT NULL COMMENT '课程班级Id',
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `studentId` int(11) DEFAULT NULL COMMENT '学生Id',
  `videoLearned` int(11) DEFAULT '0' COMMENT '已学视频数量',
  `videoCount` int(11) DEFAULT '0' COMMENT '需学视频数量',
  `lastNodeId` int(11) DEFAULT '0' COMMENT '上次学习节点',
  `workLearned` int(11) DEFAULT '0' COMMENT '已学作业',
  `workCount` int(11) DEFAULT '0' COMMENT '作业数量',
  `examLearned` int(11) DEFAULT '0' COMMENT '已学考试',
  `examCount` int(11) DEFAULT '0' COMMENT '考试数量',
  `discussJoin` int(11) DEFAULT '0' COMMENT '参与讨论数',
  `discussCount` int(11) DEFAULT '0' COMMENT '讨论数量',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `studyTime` int(11) DEFAULT '0' COMMENT '总学习时长',
  `change` tinyint(1) DEFAULT '0' COMMENT '有记录更新',
  `calculate` tinyint(1) DEFAULT '0' COMMENT '可以计算成绩',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`studentId`) USING BTREE,
  KEY `classId` (`classId`),
  KEY `studentId` (`studentId`),
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3700626 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='选课学生';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_total`
--

DROP TABLE IF EXISTS `yee_course_total`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_total` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) NOT NULL,
  `date` date NOT NULL,
  `schoolId` int(11) NOT NULL,
  `total` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=174 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_course_view_record`
--

DROP TABLE IF EXISTS `yee_course_view_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_course_view_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT '0' COMMENT '课程ID',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `frequency` int(11) DEFAULT '0' COMMENT '次数',
  `lastTime` datetime DEFAULT NULL COMMENT '最后查看时间',
  `pcQty` int(11) DEFAULT '0' COMMENT 'PC查看',
  `mbQty` int(11) DEFAULT '0' COMMENT '移动端查看',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=2937985 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='课程查看记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_default_score_rule`
--

DROP TABLE IF EXISTS `yee_default_score_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_default_score_rule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `useVideo` tinyint(1) DEFAULT NULL COMMENT '使用视频',
  `videoRatio` int(11) DEFAULT NULL COMMENT '视频比例',
  `useDiscuss` tinyint(1) DEFAULT NULL COMMENT '讨论',
  `discussRatio` int(11) DEFAULT NULL COMMENT '讨论比例',
  `discussItems` json DEFAULT NULL COMMENT '讨论计分',
  `useWork` tinyint(1) DEFAULT NULL COMMENT '作业',
  `workRatio` int(11) DEFAULT NULL COMMENT '作业比例',
  `workItems` json DEFAULT NULL COMMENT '作业计分',
  `useExam` tinyint(1) DEFAULT NULL COMMENT '考试',
  `examRatio` int(11) DEFAULT NULL COMMENT '考试比例',
  `examItems` json DEFAULT NULL COMMENT '作业计分',
  `useExtra` tinyint(1) DEFAULT NULL COMMENT '额外',
  `extraRatio` int(11) DEFAULT NULL COMMENT '额外比例',
  `addTime` datetime DEFAULT NULL COMMENT '添加时间',
  `calcNumber` int(11) DEFAULT NULL COMMENT '计算次数',
  `name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `videoItems` json DEFAULT NULL COMMENT '视频计分',
  `updateTime` datetime DEFAULT NULL COMMENT '更新时间',
  `videoMode` int(11) DEFAULT '0' COMMENT '视频分配模式',
  `description` text COMMENT '规则说明',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1001644 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='默认计分规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_discuss`
--

DROP TABLE IF EXISTS `yee_discuss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_discuss` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL COMMENT '讨论主题',
  `teacherId` int(11) DEFAULT NULL COMMENT '老师ID',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `content` mediumtext COMMENT '评论内容',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `classId` int(11) DEFAULT '0' COMMENT '选择班级',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程iD',
  `top` int(11) DEFAULT '0' COMMENT '置顶',
  `files` json DEFAULT NULL COMMENT '上传附件',
  `isDelete` tinyint(4) DEFAULT '0' COMMENT '删除',
  `changeTime` int(11) DEFAULT '0' COMMENT '更改时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1027265 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='主题讨论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_discuss_reply`
--

DROP TABLE IF EXISTS `yee_discuss_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_discuss_reply` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `discussId` int(11) DEFAULT NULL COMMENT '评论Id',
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户ID',
  `content` mediumtext COMMENT '回复内容',
  `addTime` datetime DEFAULT NULL COMMENT '回复时间',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `files` json DEFAULT NULL COMMENT '上传附件',
  `pid` int(11) DEFAULT NULL COMMENT '父Id',
  `reUserId` int(11) DEFAULT NULL COMMENT '回复人Id',
  `classId` int(11) DEFAULT '0' COMMENT '选择班级',
  `replyId` int(11) DEFAULT NULL COMMENT '回复帖子Id',
  `isDelete` tinyint(4) DEFAULT '0' COMMENT '删除',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `replyId` (`replyId`) USING BTREE,
  KEY `course_reply` (`courseId`,`replyId`,`isDelete`) USING BTREE,
  KEY `course_discuss` (`courseId`,`discussId`,`replyId`,`isDelete`) USING BTREE,
  KEY `discussId` (`discussId`,`replyId`,`isDelete`) USING BTREE,
  KEY `userId` (`userId`) USING BTREE,
  KEY `school` (`schoolId`,`isDelete`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3986934 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='评论回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_discuss_score`
--

DROP TABLE IF EXISTS `yee_discuss_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_discuss_score` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程',
  `discussId` int(11) DEFAULT NULL COMMENT '讨论',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `score` decimal(18,2) DEFAULT NULL COMMENT '得分',
  `classId` int(11) DEFAULT NULL COMMENT '班级',
  `postQty` int(11) DEFAULT '0' COMMENT '主贴数量',
  `replyQty` int(11) DEFAULT '0' COMMENT '回复数量',
  `likeQty` int(11) DEFAULT '0' COMMENT '点赞数量',
  `scored` tinyint(1) DEFAULT '0' COMMENT '已打分',
  `rank` int(11) DEFAULT '0' COMMENT '排名',
  `userType` int(11) DEFAULT '0' COMMENT '用户类型',
  `allQty` int(11) DEFAULT '0' COMMENT '总发帖数',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`discussId`,`userId`) USING BTREE,
  KEY `discussId` (`discussId`,`classId`) USING BTREE,
  KEY `course_discuss_class` (`courseId`,`discussId`,`classId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3114448 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='讨论得分';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_down_record`
--

DROP TABLE IF EXISTS `yee_down_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_down_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT '0' COMMENT '用户Id',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `downTime` datetime DEFAULT NULL COMMENT '下载时间',
  `platform` varchar(10) DEFAULT '' COMMENT '平台',
  `fileId` int(11) DEFAULT '0' COMMENT '文件Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1003953 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='下载记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam`
--

DROP TABLE IF EXISTS `yee_exam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `title` varchar(100) DEFAULT NULL COMMENT '测验标题',
  `topicNumber` int(11) DEFAULT NULL COMMENT '题目数量',
  `score` int(11) DEFAULT NULL COMMENT '总分数',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `nodeId` int(11) DEFAULT NULL COMMENT '所在节点',
  `courseId` int(11) DEFAULT NULL COMMENT '所在课程',
  `limitedTime` int(11) DEFAULT NULL COMMENT '限时',
  `sequence` int(11) DEFAULT NULL COMMENT '试题顺序',
  `remarks` mediumtext COMMENT '备注',
  `paperId` int(11) DEFAULT NULL COMMENT '选择试卷',
  `startTime` int(11) DEFAULT NULL COMMENT '开始时间',
  `endTime` int(11) DEFAULT NULL COMMENT '结束时间',
  `createUserId` int(11) DEFAULT NULL COMMENT '创建人',
  `classList` json DEFAULT NULL COMMENT '选择班级',
  `isPrivate` int(11) DEFAULT '0' COMMENT '考试班级',
  `teacherType` int(11) DEFAULT NULL COMMENT '老师类型',
  `allow` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `frequency` int(11) DEFAULT NULL COMMENT '次数',
  `hasCollect` tinyint(1) DEFAULT '0' COMMENT '已收卷',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `parsing` tinyint(1) DEFAULT '0' COMMENT '显示解析',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  `random` tinyint(4) DEFAULT '0' COMMENT '是否随机抽题',
  `randData` json DEFAULT NULL COMMENT '随机抽题设置',
  `randNumber` int(11) DEFAULT NULL COMMENT '实际抽题总数量',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`nodeId`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1008191 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam_answer`
--

DROP TABLE IF EXISTS `yee_exam_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam_answer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `recordId` int(11) DEFAULT NULL COMMENT '记录Id',
  `examId` int(11) DEFAULT NULL COMMENT '试卷Id',
  `topicId` int(11) DEFAULT NULL COMMENT '题目Id',
  `answered` tinyint(1) DEFAULT NULL COMMENT '已答题',
  `score` decimal(18,2) DEFAULT NULL COMMENT '得分',
  `answer` mediumtext COMMENT '答案',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `files` json DEFAULT NULL COMMENT '上传文件',
  `marked` varchar(100) DEFAULT NULL COMMENT '已批阅',
  `remark` mediumtext COMMENT '老师评语',
  `hit` tinyint(1) DEFAULT '0' COMMENT '0 未处理,1 全部正确,2 部分正确,3全部错误',
  `userId` int(11) DEFAULT '0' COMMENT '用户Id',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `recordId` (`recordId`) USING BTREE,
  KEY `examId` (`examId`) USING BTREE,
  KEY `topicId` (`topicId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25909773 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试答题';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam_collect`
--

DROP TABLE IF EXISTS `yee_exam_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam_collect` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `examId` int(11) DEFAULT NULL COMMENT '考试Id',
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `collectTime` datetime DEFAULT NULL COMMENT '收卷时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1005578 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam_record`
--

DROP TABLE IF EXISTS `yee_exam_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `examId` int(11) DEFAULT NULL COMMENT '考试Id',
  `userId` int(11) DEFAULT NULL COMMENT '学生Id',
  `startTime` int(11) DEFAULT NULL COMMENT '开始时间',
  `state` tinyint(1) DEFAULT '0' COMMENT '是否结束',
  `finishTime` int(11) DEFAULT NULL COMMENT '结束时间',
  `score` decimal(18,2) DEFAULT NULL COMMENT '得分',
  `isCancel` tinyint(1) DEFAULT NULL COMMENT '取消',
  `frequency` int(11) DEFAULT NULL COMMENT '次数',
  `teacherId` int(11) DEFAULT NULL COMMENT '老师',
  `markTime` int(11) DEFAULT '0' COMMENT '批阅时间',
  `obScore` decimal(18,2) DEFAULT '0.00' COMMENT '客观得分',
  `subScore` decimal(18,2) DEFAULT '0.00' COMMENT '主观题得分',
  `markOrder` int(11) DEFAULT '0' COMMENT '改卷顺序',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `classId` int(11) DEFAULT '0' COMMENT '班级Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `redo` int(11) DEFAULT '0',
  `addDate` date GENERATED ALWAYS AS (date_format(from_unixtime(`startTime`),_utf8mb4'%Y-%m-%d')) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `examId` (`examId`,`userId`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1819356 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam_score`
--

DROP TABLE IF EXISTS `yee_exam_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam_score` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `examId` int(11) DEFAULT NULL COMMENT '考试Id',
  `userId` int(11) DEFAULT NULL COMMENT '学生Id',
  `finalScore` decimal(18,2) DEFAULT NULL COMMENT '最终得分',
  `state` int(11) DEFAULT '0' COMMENT '状态',
  `scored` tinyint(1) DEFAULT '0' COMMENT '已打分',
  `submitTime` int(11) DEFAULT '0' COMMENT '提交时间',
  `timeCost` int(11) DEFAULT '0' COMMENT '用时',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `courseId` int(11) DEFAULT '0' COMMENT '课程ID',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `examId` (`examId`,`userId`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1863031 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试成绩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_exam_topic`
--

DROP TABLE IF EXISTS `yee_exam_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_exam_topic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `topic` mediumtext COMMENT '题干',
  `type` int(11) DEFAULT NULL COMMENT '试题类型',
  `level` int(11) DEFAULT NULL COMMENT '难度等级',
  `score` int(11) DEFAULT NULL COMMENT '默认分值',
  `missScore` json DEFAULT NULL COMMENT '漏选分值',
  `option1` json DEFAULT NULL COMMENT '单选选项',
  `option2` json DEFAULT NULL COMMENT '多选选项',
  `option3` json DEFAULT NULL COMMENT '判断选项',
  `analysis` mediumtext COMMENT '题目解析',
  `pid` int(11) DEFAULT NULL COMMENT '父Id',
  `examId` int(11) DEFAULT NULL COMMENT '试卷Id',
  `title` varchar(500) DEFAULT NULL COMMENT '标识',
  `oid` int(11) DEFAULT NULL COMMENT '旧id',
  `number` int(11) DEFAULT '0' COMMENT '序号',
  `upload` varchar(200) DEFAULT NULL COMMENT '上传附件',
  `option` json DEFAULT NULL COMMENT '题目选项',
  `scoreMode` int(11) DEFAULT NULL COMMENT '计分模式',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `type` (`type`) USING BTREE,
  KEY `examId` (`examId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1238108 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='考试题目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_happy_circle`
--

DROP TABLE IF EXISTS `yee_happy_circle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_happy_circle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `content` mediumtext COMMENT '评论内容',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `files` json DEFAULT NULL COMMENT '上传附件',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `replyId` int(11) DEFAULT NULL COMMENT '第一层回复Id',
  `reUserId` int(11) DEFAULT NULL COMMENT '被回复用户Id',
  `isDelete` tinyint(4) DEFAULT '0' COMMENT '删除',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schoolId` (`schoolId`,`isDelete`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10381 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='乐学圈';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_happy_reply_like`
--

DROP TABLE IF EXISTS `yee_happy_reply_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_happy_reply_like` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `replyId` int(11) DEFAULT NULL COMMENT '回复Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9936 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='乐学圈点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_live_join`
--

DROP TABLE IF EXISTS `yee_live_join`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_live_join` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `roomId` int(11) DEFAULT '0' COMMENT '课程Id',
  `state` int(1) DEFAULT '0' COMMENT '学习状态',
  `finalTime` datetime DEFAULT NULL COMMENT '最后完成时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `classId` int(11) DEFAULT NULL COMMENT '签到班级',
  `addTime` datetime DEFAULT NULL,
  `liveName` char(100) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `nodeId` (`userId`) USING BTREE,
  KEY `courseId` (`roomId`,`userId`,`state`) USING BTREE,
  KEY `userId` (`userId`,`schoolId`),
  KEY `finalTime` (`finalTime`) USING BTREE,
  KEY `join` (`roomId`,`userId`,`classId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='学习时间统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_lock`
--

DROP TABLE IF EXISTS `yee_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `lock` int(2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_login_error`
--

DROP TABLE IF EXISTS `yee_login_error`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_login_error` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `ip` varchar(30) COLLATE utf8mb4_bin DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `platform` int(11) DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `agent` text COLLATE utf8mb4_bin,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`,`ip`,`time`),
  KEY `ip` (`ip`,`time`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_manage`
--

DROP TABLE IF EXISTS `yee_manage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_manage` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `account` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '账号名称',
  `password` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '账号密码',
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '真实姓名',
  `errorCount` int(11) DEFAULT NULL COMMENT '错误次数',
  `errorTime` int(11) DEFAULT '0' COMMENT '错误时间',
  `thisTime` datetime DEFAULT NULL COMMENT '当前登录时间',
  `lastTime` datetime DEFAULT NULL COMMENT '上次登录时间',
  `thisIp` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '当前登录IP',
  `lastIp` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '上次登录IP',
  `isLock` tinyint(1) DEFAULT NULL COMMENT '锁定',
  `email` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮箱',
  `role` int(11) DEFAULT NULL COMMENT '角色',
  `avatar` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '头像',
  `mobile` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '手机号',
  `gender` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '性别',
  `weChat` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '微信号',
  `intro` text CHARACTER SET utf8 COMMENT '简介',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `super` tinyint(1) DEFAULT '0' COMMENT '超级',
  `collegeId` int(11) DEFAULT NULL COMMENT '所属学院',
  `general` tinyint(1) DEFAULT NULL,
  `loginCode` varchar(255) DEFAULT NULL,
  `recommend` tinyint(1) DEFAULT '0' COMMENT '推荐到首页',
  `active` tinyint(1) DEFAULT '0' COMMENT '激活',
  `colleges` json DEFAULT NULL COMMENT '兼职院校',
  `addTime` datetime DEFAULT NULL,
  `force` int(11) DEFAULT '0' COMMENT '绑定微信',
  `passport` varchar(100) DEFAULT NULL COMMENT '通行秘钥',
  `bindId` int(11) NOT NULL DEFAULT '0' COMMENT '绑定主站Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `account` (`account`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1283011 DEFAULT CHARSET=utf8mb4 COMMENT='账号管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_member`
--

DROP TABLE IF EXISTS `yee_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_member` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '名字',
  `type` int(11) DEFAULT NULL COMMENT '类型',
  `email` varchar(255) DEFAULT NULL,
  `avatar` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '头像',
  `pushId` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT 'PushId',
  `platform` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `token` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1283006 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_muted`
--

DROP TABLE IF EXISTS `yee_muted`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_muted` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '禁言用户',
  `unlockTime` int(11) DEFAULT NULL COMMENT '解锁时间',
  `forum` varchar(50) DEFAULT NULL COMMENT '讨论区',
  `teacherId` int(11) DEFAULT NULL COMMENT '操作人',
  `addTime` datetime DEFAULT NULL COMMENT '禁言时间',
  `content` text COMMENT '发言内容',
  `schoolId` int(11) DEFAULT NULL COMMENT '学校ID',
  `replyId` int(11) DEFAULT NULL COMMENT '回复id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='禁言';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_node`
--

DROP TABLE IF EXISTS `yee_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) CHARACTER SET utf8 DEFAULT NULL COMMENT '节名称',
  `type` json DEFAULT NULL COMMENT '类型',
  `chapterId` int(11) DEFAULT NULL COMMENT '所属章',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `videoFile` varchar(300) DEFAULT NULL COMMENT '视频文件',
  `videoDuration` int(11) DEFAULT NULL COMMENT '视频时长',
  `votingPath` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '投票路径',
  `tabVideo` int(11) DEFAULT '0' COMMENT '类型',
  `tabFile` int(11) DEFAULT '0' COMMENT '类型',
  `tabVote` int(11) DEFAULT '0' COMMENT '类型',
  `tabWork` int(11) DEFAULT '0' COMMENT '类型',
  `tabExam` int(11) DEFAULT '0' COMMENT '类型',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `videoMode` int(11) DEFAULT NULL COMMENT '视频模式',
  `localFile` varchar(300) DEFAULT NULL COMMENT '本地视频',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `lock` tinyint(1) DEFAULT '0' COMMENT '时间锁',
  `unlockTime` int(11) DEFAULT '0' COMMENT '解锁时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`tabVideo`) USING BTREE,
  KEY `chapterId` (`chapterId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1425193 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='课程节';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_node_discuss`
--

DROP TABLE IF EXISTS `yee_node_discuss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_node_discuss` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `content` mediumtext COMMENT '评论内容',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `nodeId` int(11) DEFAULT '0' COMMENT '节点Id',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程iD',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `replyId` int(11) DEFAULT NULL COMMENT '第一层回复Id',
  `reUserId` int(11) DEFAULT NULL COMMENT '被回复用户Id',
  `files` json DEFAULT NULL COMMENT '上传附件',
  `isDelete` tinyint(4) DEFAULT '0' COMMENT '删除',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `replyId` (`replyId`) USING BTREE,
  KEY `nodeId` (`courseId`,`nodeId`,`replyId`) USING BTREE,
  KEY `schoolId` (`schoolId`,`isDelete`) USING BTREE,
  KEY `userId` (`courseId`,`userId`),
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4282755 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='章节讨论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_node_files`
--

DROP TABLE IF EXISTS `yee_node_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_node_files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nodeId` int(11) DEFAULT NULL COMMENT '所属节点',
  `courseId` int(11) DEFAULT NULL COMMENT '所属课程',
  `name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '文件名称',
  `uploadPath` varchar(300) DEFAULT NULL COMMENT '上传路径',
  `timeView` int(11) DEFAULT '0' COMMENT '查看次数',
  `createUserId` int(11) unsigned zerofill DEFAULT NULL COMMENT '上传人',
  `addTime` datetime DEFAULT NULL COMMENT '上传时间',
  `fileName` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '文件名称',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1035504 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='节点资料';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_node_reply_like`
--

DROP TABLE IF EXISTS `yee_node_reply_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_node_reply_like` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `replyId` int(11) DEFAULT NULL COMMENT '回复Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `replyId` (`replyId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=142969 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='章节评论点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_notice`
--

DROP TABLE IF EXISTS `yee_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `type` int(11) DEFAULT NULL COMMENT '推送类型',
  `classIds` json DEFAULT NULL COMMENT '选择班级',
  `userNumber` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '学号',
  `title` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '消息标题',
  `summary` text CHARACTER SET utf8 COMMENT '消息摘要',
  `content` text CHARACTER SET utf8 COMMENT '消息内容',
  `userId` int(11) DEFAULT NULL COMMENT '学生Id',
  `addTime` datetime DEFAULT NULL COMMENT '发送时间',
  `isPush` int(11) DEFAULT '0' COMMENT '推送',
  `pushTime` datetime DEFAULT NULL COMMENT '推送时间',
  `sysPush` tinyint(1) DEFAULT '0' COMMENT '系统推送',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1002574 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='消息通知';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_notice_queue`
--

DROP TABLE IF EXISTS `yee_notice_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_notice_queue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `noticeId` int(11) DEFAULT NULL COMMENT '通知Id',
  `pushed` tinyint(1) DEFAULT '0' COMMENT '是否已推送',
  `isRead` tinyint(1) DEFAULT '0' COMMENT '是否已读取',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `noticeId` (`noticeId`,`userId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=36209 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='通知队列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_online`
--

DROP TABLE IF EXISTS `yee_online`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_online` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT '0' COMMENT '登录账号',
  `logInTime` int(11) DEFAULT NULL COMMENT '登录时间',
  `lastTime` int(11) DEFAULT '0' COMMENT '最后活跃时间',
  `platform` varchar(10) DEFAULT '' COMMENT '平台',
  `ip` int(11) unsigned DEFAULT '0' COMMENT 'IP',
  `duration` int(11) DEFAULT '0' COMMENT '时长',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `loginTime2` datetime GENERATED ALWAYS AS (date_format(from_unixtime(`logInTime`),_utf8mb4'%Y-%m-%d %H:%i:%s')) VIRTUAL,
  `lastTime2` datetime GENERATED ALWAYS AS (date_format(from_unixtime(`lastTime`),_utf8mb4'%Y-%m-%d %H:%i:%s')) VIRTUAL,
  `ip2` varchar(50) GENERATED ALWAYS AS (inet_ntoa(`ip`)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `logInTime` (`logInTime`) USING BTREE,
  KEY `userId` (`userId`,`ip`,`lastTime`) USING BTREE,
  KEY `lastTime` (`lastTime`,`logInTime`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=33117653 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='在线记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_paper`
--

DROP TABLE IF EXISTS `yee_paper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_paper` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `title` varchar(300) DEFAULT NULL COMMENT '试卷标题',
  `topicNumber` int(11) DEFAULT NULL COMMENT '题目数量',
  `score` int(11) DEFAULT NULL COMMENT '总分数',
  `type` int(11) DEFAULT NULL COMMENT '类型',
  `scope` varchar(100) DEFAULT NULL COMMENT '适用年级/范围',
  `remarks` mediumtext COMMENT '备注',
  `allow` tinyint(1) DEFAULT '0' COMMENT '审核启用',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='试卷';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_paper_topic`
--

DROP TABLE IF EXISTS `yee_paper_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_paper_topic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `oid` int(11) NOT NULL,
  `topic` mediumtext COMMENT '题干',
  `type` int(11) DEFAULT NULL COMMENT '试题类型',
  `level` int(11) DEFAULT NULL COMMENT '难度等级',
  `score` int(11) DEFAULT NULL COMMENT '默认分值',
  `missScore` json DEFAULT NULL COMMENT '漏选分值',
  `option1` json DEFAULT NULL COMMENT '单选选项',
  `option2` json DEFAULT NULL COMMENT '多选选项',
  `option3` json DEFAULT NULL COMMENT '判断选项',
  `analysis` mediumtext COMMENT '题目解析',
  `pid` int(11) DEFAULT NULL COMMENT '父Id',
  `paperId` int(11) DEFAULT NULL COMMENT '试卷Id',
  `title` varchar(200) DEFAULT NULL COMMENT '标识',
  `upload` varchar(200) DEFAULT NULL COMMENT '上传附件',
  `option` json DEFAULT NULL COMMENT '题目选项',
  `scoreMode` int(11) DEFAULT NULL COMMENT '计分模式',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  `number` int(11) DEFAULT '0' COMMENT '序号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=122714 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='试卷题目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_question`
--

DROP TABLE IF EXISTS `yee_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_question` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `topic` mediumtext COMMENT '题干',
  `type` int(11) DEFAULT NULL COMMENT '试题类型',
  `level` int(11) DEFAULT NULL COMMENT '难度等级',
  `score` int(11) DEFAULT NULL COMMENT '默认分值',
  `missScore` json DEFAULT NULL COMMENT '漏选分值',
  `analysis` mediumtext COMMENT '题目解析',
  `pid` int(11) DEFAULT NULL COMMENT '父Id',
  `title` varchar(100) DEFAULT NULL COMMENT '标识',
  `oid` int(11) DEFAULT '0',
  `upload` varchar(200) DEFAULT NULL COMMENT '上传附件',
  `option` json DEFAULT NULL COMMENT '题目选项',
  `scoreMode` int(11) DEFAULT NULL COMMENT '计分模式',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  `createId` int(11) DEFAULT '0' COMMENT '创建人',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schoolId` (`schoolId`,`createId`),
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=117416 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='试题题库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_recycle_bin`
--

DROP TABLE IF EXISTS `yee_recycle_bin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_recycle_bin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tableName` varchar(100) DEFAULT NULL COMMENT '表名',
  `deleteTime` datetime DEFAULT NULL COMMENT '删除时间',
  `userId` int(11) DEFAULT '0' COMMENT '操作人',
  `data` json DEFAULT NULL COMMENT '数据',
  `batchNum` int(11) DEFAULT NULL COMMENT '批次号',
  `condition` text COMMENT '删除条件',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `batchNum` (`batchNum`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_reply_like`
--

DROP TABLE IF EXISTS `yee_reply_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_reply_like` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `replyId` int(11) DEFAULT NULL COMMENT '回复Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `replyId` (`replyId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=307841 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_report_date`
--

DROP TABLE IF EXISTS `yee_report_date`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_report_date` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schoolId` int(11) DEFAULT '0',
  `date` date DEFAULT NULL,
  `online` int(11) DEFAULT '0',
  `study` int(11) DEFAULT '0',
  `online2` int(11) DEFAULT NULL,
  `study2` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `date` (`date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7678 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='按天统计在线，学时表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_report_fragment`
--

DROP TABLE IF EXISTS `yee_report_fragment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_report_fragment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schoolId` int(11) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `online` int(11) DEFAULT '0' COMMENT '在线人数',
  `study` int(11) DEFAULT '0' COMMENT '在线人数',
  `online2` int(11) DEFAULT NULL COMMENT '在线人次',
  `study2` int(11) DEFAULT NULL COMMENT '学习人次',
  `time` time DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `df` (`date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=233904 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='分时段20分钟统计在线，学习记录表。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_role_auth`
--

DROP TABLE IF EXISTS `yee_role_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_role_auth` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `roleId` int(11) DEFAULT NULL COMMENT '角色Id',
  `authId` int(11) DEFAULT '0' COMMENT '权限节点Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `roleId` (`roleId`,`authId`,`schoolId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=34505 DEFAULT CHARSET=utf8mb4 COMMENT='权限控制';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_school_column`
--

DROP TABLE IF EXISTS `yee_school_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_school_column` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT '栏目名称',
  `type` int(11) DEFAULT '0' COMMENT '类型',
  `more` varchar(255) DEFAULT NULL COMMENT '更多链接',
  `allow` tinyint(1) DEFAULT '0' COMMENT '审核',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `data` json DEFAULT NULL,
  `addTime` datetime DEFAULT NULL,
  `schoolId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_school_date_record`
--

DROP TABLE IF EXISTS `yee_school_date_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_school_date_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schoolId` int(11) NOT NULL DEFAULT '0',
  `student` int(11) NOT NULL DEFAULT '0',
  `student_inc` int(11) NOT NULL DEFAULT '0',
  `student_dec` int(11) NOT NULL DEFAULT '0',
  `teacher` int(11) NOT NULL DEFAULT '0',
  `teacher_inc` int(11) NOT NULL DEFAULT '0',
  `teacher_dec` int(11) NOT NULL DEFAULT '0',
  `discuss` int(11) NOT NULL DEFAULT '0',
  `discuss_inc` int(11) NOT NULL DEFAULT '0',
  `discuss_dec` int(11) NOT NULL DEFAULT '0',
  `reply` int(11) NOT NULL DEFAULT '0',
  `reply_inc` int(11) NOT NULL DEFAULT '0',
  `reply_dec` int(11) NOT NULL DEFAULT '0',
  `replyMa` int(11) NOT NULL DEFAULT '0',
  `replyMa_inc` int(11) NOT NULL DEFAULT '0',
  `replyMa_dec` int(11) NOT NULL DEFAULT '0',
  `replyRe` int(11) NOT NULL DEFAULT '0',
  `replyRe_inc` int(11) NOT NULL DEFAULT '0',
  `replyRe_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReply` int(11) NOT NULL DEFAULT '0',
  `nodeReply_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReply_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe_dec` int(11) NOT NULL DEFAULT '0',
  `happyReply` int(11) NOT NULL DEFAULT '0',
  `happyReply_inc` int(11) NOT NULL DEFAULT '0',
  `happyReply_dec` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa_inc` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa_dec` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe_inc` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe_dec` int(11) NOT NULL DEFAULT '0',
  `course` int(11) NOT NULL DEFAULT '0',
  `course_inc` int(11) NOT NULL DEFAULT '0',
  `course_dec` int(11) NOT NULL DEFAULT '0',
  `courseEnable` int(11) NOT NULL DEFAULT '0',
  `courseEnable_inc` int(11) NOT NULL DEFAULT '0',
  `courseEnable_dec` int(11) NOT NULL DEFAULT '0',
  `courseDisable` int(11) NOT NULL DEFAULT '0',
  `courseDisable_inc` int(11) NOT NULL DEFAULT '0',
  `courseDisable_dec` int(11) NOT NULL DEFAULT '0',
  `course_student` int(11) NOT NULL DEFAULT '0',
  `course_student_inc` int(11) NOT NULL DEFAULT '0',
  `course_student_dec` int(11) NOT NULL DEFAULT '0',
  `courseStuEle` int(11) NOT NULL DEFAULT '0',
  `courseStuEle_inc` int(11) NOT NULL DEFAULT '0',
  `courseStuEle_dec` int(11) NOT NULL DEFAULT '0',
  `courseStuReq` int(11) NOT NULL DEFAULT '0',
  `courseStuReq_inc` int(11) NOT NULL DEFAULT '0',
  `courseStuReq_dec` int(11) NOT NULL DEFAULT '0',
  `course_class` int(11) NOT NULL DEFAULT '0',
  `course_class_inc` int(11) NOT NULL DEFAULT '0',
  `course_class_dec` int(11) NOT NULL DEFAULT '0',
  `classes` int(11) NOT NULL DEFAULT '0',
  `classes_inc` int(11) NOT NULL DEFAULT '0',
  `classes_dec` int(11) NOT NULL DEFAULT '0',
  `paper` int(11) NOT NULL DEFAULT '0',
  `paper_inc` int(11) NOT NULL DEFAULT '0',
  `paper_dec` int(11) NOT NULL DEFAULT '0',
  `paperEnable` int(11) NOT NULL DEFAULT '0',
  `paperEnable_inc` int(11) NOT NULL DEFAULT '0',
  `paperEnable_dec` int(11) NOT NULL DEFAULT '0',
  `paperDisable` int(11) NOT NULL DEFAULT '0',
  `paperDisable_inc` int(11) NOT NULL DEFAULT '0',
  `paperDisable_dec` int(11) NOT NULL DEFAULT '0',
  `question` int(11) NOT NULL DEFAULT '0',
  `question_inc` int(11) NOT NULL DEFAULT '0',
  `question_dec` int(11) NOT NULL DEFAULT '0',
  `work` int(11) NOT NULL DEFAULT '0',
  `work_inc` int(11) NOT NULL DEFAULT '0',
  `work_dec` int(11) NOT NULL DEFAULT '0',
  `workEnable` int(11) NOT NULL DEFAULT '0',
  `workEnable_inc` int(11) NOT NULL DEFAULT '0',
  `workEnable_dec` int(11) NOT NULL DEFAULT '0',
  `workDisable` int(11) NOT NULL DEFAULT '0',
  `workDisable_inc` int(11) NOT NULL DEFAULT '0',
  `workDisable_dec` int(11) NOT NULL DEFAULT '0',
  `workRecord` int(11) NOT NULL DEFAULT '0',
  `workRecord_inc` int(11) NOT NULL DEFAULT '0',
  `workRecord_dec` int(11) NOT NULL DEFAULT '0',
  `exam` int(11) NOT NULL DEFAULT '0',
  `exam_inc` int(11) NOT NULL DEFAULT '0',
  `exam_dec` int(11) NOT NULL DEFAULT '0',
  `examEnable` int(11) NOT NULL DEFAULT '0',
  `examEnable_inc` int(11) NOT NULL DEFAULT '0',
  `examEnable_dec` int(11) NOT NULL DEFAULT '0',
  `examDisable` int(11) NOT NULL DEFAULT '0',
  `examDisable_inc` int(11) NOT NULL DEFAULT '0',
  `examDisable_dec` int(11) NOT NULL DEFAULT '0',
  `examRecord` int(11) NOT NULL DEFAULT '0',
  `examRecord_inc` int(11) NOT NULL DEFAULT '0',
  `examRecord_dec` int(11) NOT NULL DEFAULT '0',
  `updateTime` int(11) NOT NULL DEFAULT '0',
  `addDate` date DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `schoolId` (`schoolId`,`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_school_date_total`
--

DROP TABLE IF EXISTS `yee_school_date_total`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_school_date_total` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schoolId` int(11) NOT NULL DEFAULT '0',
  `student` int(11) NOT NULL DEFAULT '0',
  `student_inc` int(11) NOT NULL DEFAULT '0',
  `student_dec` int(11) NOT NULL DEFAULT '0',
  `teacher` int(11) NOT NULL DEFAULT '0',
  `teacher_inc` int(11) NOT NULL DEFAULT '0',
  `teacher_dec` int(11) NOT NULL DEFAULT '0',
  `discuss` int(11) NOT NULL DEFAULT '0',
  `discuss_inc` int(11) NOT NULL DEFAULT '0',
  `discuss_dec` int(11) NOT NULL DEFAULT '0',
  `reply` int(11) NOT NULL DEFAULT '0',
  `reply_inc` int(11) NOT NULL DEFAULT '0',
  `reply_dec` int(11) NOT NULL DEFAULT '0',
  `replyMa` int(11) NOT NULL DEFAULT '0',
  `replyMa_inc` int(11) NOT NULL DEFAULT '0',
  `replyMa_dec` int(11) NOT NULL DEFAULT '0',
  `replyRe` int(11) NOT NULL DEFAULT '0',
  `replyRe_inc` int(11) NOT NULL DEFAULT '0',
  `replyRe_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReply` int(11) NOT NULL DEFAULT '0',
  `nodeReply_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReply_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReplyMa_dec` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe_inc` int(11) NOT NULL DEFAULT '0',
  `nodeReplyRe_dec` int(11) NOT NULL DEFAULT '0',
  `happyReply` int(11) NOT NULL DEFAULT '0',
  `happyReply_inc` int(11) NOT NULL DEFAULT '0',
  `happyReply_dec` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa_inc` int(11) NOT NULL DEFAULT '0',
  `happyReplyMa_dec` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe_inc` int(11) NOT NULL DEFAULT '0',
  `happyReplyRe_dec` int(11) NOT NULL DEFAULT '0',
  `course` int(11) NOT NULL DEFAULT '0',
  `course_inc` int(11) NOT NULL DEFAULT '0',
  `course_dec` int(11) NOT NULL DEFAULT '0',
  `courseEnable` int(11) NOT NULL DEFAULT '0',
  `courseEnable_inc` int(11) NOT NULL DEFAULT '0',
  `courseEnable_dec` int(11) NOT NULL DEFAULT '0',
  `courseDisable` int(11) NOT NULL DEFAULT '0',
  `courseDisable_inc` int(11) NOT NULL DEFAULT '0',
  `courseDisable_dec` int(11) NOT NULL DEFAULT '0',
  `course_student` int(11) NOT NULL DEFAULT '0',
  `course_student_inc` int(11) NOT NULL DEFAULT '0',
  `course_student_dec` int(11) NOT NULL DEFAULT '0',
  `courseStuEle` int(11) NOT NULL DEFAULT '0',
  `courseStuEle_inc` int(11) NOT NULL DEFAULT '0',
  `courseStuEle_dec` int(11) NOT NULL DEFAULT '0',
  `courseStuReq` int(11) NOT NULL DEFAULT '0',
  `courseStuReq_inc` int(11) NOT NULL DEFAULT '0',
  `courseStuReq_dec` int(11) NOT NULL DEFAULT '0',
  `course_class` int(11) NOT NULL DEFAULT '0',
  `course_class_inc` int(11) NOT NULL DEFAULT '0',
  `course_class_dec` int(11) NOT NULL DEFAULT '0',
  `classes` int(11) NOT NULL DEFAULT '0',
  `classes_inc` int(11) NOT NULL DEFAULT '0',
  `classes_dec` int(11) NOT NULL DEFAULT '0',
  `paper` int(11) NOT NULL DEFAULT '0',
  `paper_inc` int(11) NOT NULL DEFAULT '0',
  `paper_dec` int(11) NOT NULL DEFAULT '0',
  `paperEnable` int(11) NOT NULL DEFAULT '0',
  `paperEnable_inc` int(11) NOT NULL DEFAULT '0',
  `paperEnable_dec` int(11) NOT NULL DEFAULT '0',
  `paperDisable` int(11) NOT NULL DEFAULT '0',
  `paperDisable_inc` int(11) NOT NULL DEFAULT '0',
  `paperDisable_dec` int(11) NOT NULL DEFAULT '0',
  `question` int(11) NOT NULL DEFAULT '0',
  `question_inc` int(11) NOT NULL DEFAULT '0',
  `question_dec` int(11) NOT NULL DEFAULT '0',
  `work` int(11) NOT NULL DEFAULT '0',
  `work_inc` int(11) NOT NULL DEFAULT '0',
  `work_dec` int(11) NOT NULL DEFAULT '0',
  `workEnable` int(11) NOT NULL DEFAULT '0',
  `workEnable_inc` int(11) NOT NULL DEFAULT '0',
  `workEnable_dec` int(11) NOT NULL DEFAULT '0',
  `workDisable` int(11) NOT NULL DEFAULT '0',
  `workDisable_inc` int(11) NOT NULL DEFAULT '0',
  `workDisable_dec` int(11) NOT NULL DEFAULT '0',
  `workRecord` int(11) NOT NULL DEFAULT '0',
  `workRecord_inc` int(11) NOT NULL DEFAULT '0',
  `workRecord_dec` int(11) NOT NULL DEFAULT '0',
  `exam` int(11) NOT NULL DEFAULT '0',
  `exam_inc` int(11) NOT NULL DEFAULT '0',
  `exam_dec` int(11) NOT NULL DEFAULT '0',
  `examEnable` int(11) NOT NULL DEFAULT '0',
  `examEnable_inc` int(11) NOT NULL DEFAULT '0',
  `examEnable_dec` int(11) NOT NULL DEFAULT '0',
  `examDisable` int(11) NOT NULL DEFAULT '0',
  `examDisable_inc` int(11) NOT NULL DEFAULT '0',
  `examDisable_dec` int(11) NOT NULL DEFAULT '0',
  `examRecord` int(11) NOT NULL DEFAULT '0',
  `examRecord_inc` int(11) NOT NULL DEFAULT '0',
  `examRecord_dec` int(11) NOT NULL DEFAULT '0',
  `updateTime` int(11) NOT NULL DEFAULT '0',
  `addDate` date DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `schoolId` (`schoolId`,`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=152 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_sign_in`
--

DROP TABLE IF EXISTS `yee_sign_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_sign_in` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT NULL COMMENT '课程',
  `name` varchar(100) DEFAULT NULL COMMENT '签到任务名称',
  `teacherId` int(11) DEFAULT NULL COMMENT '创建老师',
  `classList` json DEFAULT NULL COMMENT '选择签到班级',
  `allow` tinyint(1) DEFAULT '0' COMMENT '是否审核',
  `finish` tinyint(1) DEFAULT '0' COMMENT '结束',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `signInTime` datetime DEFAULT NULL COMMENT '签到时间',
  `endTime` datetime DEFAULT NULL COMMENT '结束时间',
  `lateTime` int(11) DEFAULT '0' COMMENT '迟到时间(分钟)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9667 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='签到';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_sign_in_record`
--

DROP TABLE IF EXISTS `yee_sign_in_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_sign_in_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `signId` int(11) DEFAULT NULL COMMENT '签到Id',
  `userId` int(11) DEFAULT '0' COMMENT '签到用户',
  `signTime` datetime DEFAULT NULL COMMENT '签到时间',
  `courseId` int(11) DEFAULT NULL COMMENT '课程ID',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `state` int(4) DEFAULT '0' COMMENT '1正常，2迟到',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=734140 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='签到记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_student`
--

DROP TABLE IF EXISTS `yee_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `number` varchar(100) DEFAULT NULL COMMENT '学号',
  `name` varchar(100) DEFAULT NULL COMMENT '姓名',
  `idCard` varchar(100) DEFAULT NULL COMMENT '身份证',
  `gender` varchar(100) DEFAULT NULL COMMENT '性别',
  `entryYear` int(11) DEFAULT NULL COMMENT '录入年份',
  `mobile` varchar(100) DEFAULT NULL COMMENT '手机号码',
  `weChat` varchar(100) DEFAULT NULL COMMENT '微信号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `intro` text COMMENT '简介',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `collegeId` int(11) DEFAULT NULL COMMENT '学院Id',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `point` int(11) DEFAULT '0' COMMENT '积分排名',
  `area` json DEFAULT NULL COMMENT '所在地区',
  `province` int(11) DEFAULT '0' COMMENT '所在地区',
  `city` int(11) DEFAULT '0' COMMENT '所在地区',
  `region` int(11) unsigned DEFAULT '0' COMMENT '所在地区',
  `address` varchar(100) DEFAULT NULL COMMENT '详细地址',
  `addTime` datetime DEFAULT NULL COMMENT '录入时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `tipPass` tinyint(1) DEFAULT '0' COMMENT '提示密码',
  `signature` varchar(200) DEFAULT NULL COMMENT '个性签名',
  `studyDuration` int(11) DEFAULT NULL COMMENT '学习时长',
  `discJoin` int(11) DEFAULT NULL COMMENT '讨论参与',
  `discReply` int(11) DEFAULT NULL COMMENT '讨论回复',
  `completeCourse` int(11) DEFAULT NULL COMMENT '完成课程',
  `studyCourse` int(11) DEFAULT NULL COMMENT '全部课程',
  `circleCount` int(11) DEFAULT NULL COMMENT '乐学圈',
  `errorCount` int(11) DEFAULT '0',
  `errorTime` int(11) DEFAULT '0',
  `passport` varchar(100) DEFAULT NULL,
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `number` (`number`,`schoolId`) USING BTREE,
  KEY `classId` (`classId`) USING BTREE,
  KEY `schoolId` (`schoolId`,`collegeId`) USING BTREE,
  KEY `mobile` (`mobile`) USING BTREE,
  KEY `email` (`email`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1282991 DEFAULT CHARSET=utf8 COMMENT='学生';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_study_post`
--

DROP TABLE IF EXISTS `yee_study_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_study_post` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `nodeId` int(11) DEFAULT NULL,
  `courseId` int(11) DEFAULT NULL,
  `sendTime` datetime DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `ip` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
  `terminal` varchar(10) COLLATE utf8mb4_bin DEFAULT NULL,
  `schoolId` int(11) DEFAULT NULL,
  `studyId` int(11) DEFAULT NULL,
  `close` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_study_time`
--

DROP TABLE IF EXISTS `yee_study_time`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_study_time` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nodeId` int(11) DEFAULT NULL COMMENT '节点Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `counter` int(11) DEFAULT NULL COMMENT '计数器',
  `duration` int(11) DEFAULT '0' COMMENT '学习时长',
  `addTime` datetime DEFAULT NULL COMMENT '学习时间',
  `ip` varchar(100) DEFAULT NULL COMMENT '学习Ip',
  `terminal` varchar(100) DEFAULT NULL COMMENT '学习终端',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `beginTime` int(11) DEFAULT '0' COMMENT '开始时间',
  `lastTime` int(11) DEFAULT '0' COMMENT '最后活跃时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `post` int(11) DEFAULT '0' COMMENT '更新次数',
  `close` tinyint(1) DEFAULT '0' COMMENT '有无关闭',
  `wg` tinyint(1) DEFAULT '0' COMMENT '检测外挂',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `nodeId` (`nodeId`,`userId`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`,`terminal`) USING BTREE,
  KEY `userId` (`userId`,`addTime`) USING BTREE,
  KEY `wg` (`wg`),
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=54151308 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='学习时间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_study_total`
--

DROP TABLE IF EXISTS `yee_study_total`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_study_total` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nodeId` int(11) DEFAULT NULL COMMENT '节点Id',
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `duration` int(11) DEFAULT '0' COMMENT '学习时长',
  `progress` decimal(18,2) DEFAULT NULL COMMENT '学习进度',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `state` int(1) DEFAULT '0' COMMENT '学习状态',
  `times` int(11) DEFAULT '0' COMMENT '学习次数',
  `finalTime` int(11) DEFAULT '0' COMMENT '最后完成时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `nodeId` (`nodeId`,`userId`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`,`state`) USING BTREE,
  KEY `userId` (`userId`,`schoolId`),
  KEY `finalTime` (`finalTime`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=40457705 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='学习时间统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_task`
--

DROP TABLE IF EXISTS `yee_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `taskName` varchar(255) DEFAULT NULL,
  `updateTime` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_teacher_comment`
--

DROP TABLE IF EXISTS `yee_teacher_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_teacher_comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户',
  `kind` int(11) DEFAULT NULL COMMENT '1 作业 2 试卷',
  `testId` int(11) DEFAULT NULL COMMENT '试卷Id',
  `topicId` int(11) DEFAULT NULL COMMENT '题目id',
  `content` text COMMENT '内容',
  `addTime` datetime DEFAULT NULL COMMENT '评论时间',
  `md5` varchar(100) DEFAULT NULL COMMENT '校验MD5',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='老师评语';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_teacher_course`
--

DROP TABLE IF EXISTS `yee_teacher_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_teacher_course` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `teacherId` int(11) DEFAULT NULL COMMENT '老师Id',
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1015554 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='老师课程';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_unbind`
--

DROP TABLE IF EXISTS `yee_unbind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_unbind` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `number` int(11) DEFAULT NULL,
  `schoolId` int(11) DEFAULT NULL,
  `openId` varchar(255) DEFAULT NULL,
  `addtime` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_user_login`
--

DROP TABLE IF EXISTS `yee_user_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_user_login` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用Id',
  `loginTime` datetime DEFAULT NULL COMMENT '登录时间',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `ip` int(11) unsigned DEFAULT NULL COMMENT '登录IP',
  `num` int(11) DEFAULT NULL COMMENT '登录次数',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `userId` (`userId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7689285 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='用户登录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_user_signin`
--

DROP TABLE IF EXISTS `yee_user_signin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_user_signin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT '0' COMMENT '用Id',
  `addTime` datetime DEFAULT NULL COMMENT '签到时间',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=2856270 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='用户签到';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work`
--

DROP TABLE IF EXISTS `yee_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '用户Id',
  `title` varchar(200) DEFAULT NULL COMMENT '作业标题',
  `topicNumber` int(11) DEFAULT NULL COMMENT '题目数量',
  `score` int(11) DEFAULT NULL COMMENT '总分数',
  `type` int(11) DEFAULT NULL COMMENT '测验类型',
  `remarks` mediumtext COMMENT '备注',
  `addTime` datetime DEFAULT NULL COMMENT '创建时间',
  `sequence` int(11) DEFAULT NULL COMMENT '试题顺序',
  `nodeId` int(11) DEFAULT NULL COMMENT '所在节点',
  `courseId` int(11) DEFAULT NULL COMMENT '所在课程',
  `startTime` int(11) DEFAULT NULL COMMENT '开始时间',
  `endTime` int(11) DEFAULT NULL COMMENT '结束时间',
  `paperId` int(11) DEFAULT NULL COMMENT '选择试卷',
  `createUserId` int(11) DEFAULT NULL COMMENT '创建人',
  `isPrivate` int(11) DEFAULT '0' COMMENT '适用范围',
  `classList` json DEFAULT NULL COMMENT '选择班级',
  `teacherType` int(11) DEFAULT NULL COMMENT '老师类型',
  `allow` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `frequency` int(11) DEFAULT NULL COMMENT '答题次数',
  `scoringRules` int(11) DEFAULT NULL COMMENT '成绩规则',
  `hasCollect` tinyint(1) DEFAULT '0' COMMENT '已有收卷',
  `lock` tinyint(1) DEFAULT NULL COMMENT '锁定',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `parsing` tinyint(1) DEFAULT '0' COMMENT '显示解析',
  `addDate` date GENERATED ALWAYS AS (cast(`addTime` as date)) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `courseId` (`courseId`,`nodeId`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1019762 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='作业';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_answer`
--

DROP TABLE IF EXISTS `yee_work_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_answer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `oid` int(11) DEFAULT '0',
  `recordId` int(11) DEFAULT NULL COMMENT '记录Id',
  `workId` int(11) DEFAULT NULL COMMENT '作业Id',
  `topicId` int(11) DEFAULT NULL COMMENT '题目Id',
  `answered` tinyint(1) DEFAULT '0' COMMENT '已答题',
  `score` decimal(18,2) DEFAULT NULL COMMENT '得分',
  `answer` mediumtext COMMENT '答案',
  `images` json DEFAULT NULL COMMENT '上传图片',
  `files` json DEFAULT NULL COMMENT '上传文件',
  `marked` varchar(100) DEFAULT '0' COMMENT '已批阅',
  `remark` mediumtext COMMENT '老师评语',
  `hit` tinyint(1) DEFAULT '0' COMMENT '0 未处理,1 全部正确,2 部分正确,3全部错误',
  `userId` int(11) DEFAULT '0' COMMENT '用户Id',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `isEval` tinyint(1) DEFAULT '0' COMMENT '是否互评',
  `mistakeDelete` tinyint(1) DEFAULT '0' COMMENT '错题删除',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `recordId` (`recordId`) USING BTREE,
  KEY `workId` (`workId`) USING BTREE,
  KEY `topicId` (`topicId`) USING BTREE,
  KEY `courseId` (`courseId`,`workId`,`userId`) USING BTREE,
  KEY `schoolId` (`schoolId`)
) ENGINE=InnoDB AUTO_INCREMENT=17747072 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='作业答题';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_collect`
--

DROP TABLE IF EXISTS `yee_work_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_collect` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workId` int(11) DEFAULT NULL COMMENT '作业Id',
  `courseId` int(11) DEFAULT NULL COMMENT '课程Id',
  `classId` int(11) DEFAULT NULL COMMENT '班级Id',
  `collectTime` datetime DEFAULT NULL COMMENT '收卷时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1008741 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='作业班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_eval_class`
--

DROP TABLE IF EXISTS `yee_work_eval_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_eval_class` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `workId` int(11) DEFAULT '0' COMMENT '作业Id',
  `classId` int(11) DEFAULT '0' COMMENT '班级Id',
  `addTime` datetime DEFAULT NULL COMMENT '添加时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='作业互评班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_evaluation`
--

DROP TABLE IF EXISTS `yee_work_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_evaluation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `workId` int(11) DEFAULT '0' COMMENT '作业Id',
  `recordId` int(11) DEFAULT '0' COMMENT '答题记录Id',
  `markId` int(11) DEFAULT '0' COMMENT '评卷人',
  `markTime` int(11) DEFAULT '0' COMMENT '评卷时间',
  `subScore` decimal(18,2) DEFAULT '0.00' COMMENT '主观得分',
  `userId` int(11) DEFAULT '0' COMMENT '答题学生',
  `classId` int(11) DEFAULT '0' COMMENT '班级Id',
  `addTime` datetime DEFAULT NULL COMMENT '添加时间',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='作业互评';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_record`
--

DROP TABLE IF EXISTS `yee_work_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workId` int(11) DEFAULT NULL COMMENT '作业Id',
  `userId` int(11) DEFAULT NULL COMMENT '学生Id',
  `startTime` int(11) DEFAULT NULL COMMENT '开始时间',
  `state` int(11) DEFAULT '0' COMMENT '状态',
  `finishTime` int(11) DEFAULT NULL COMMENT '结束时间',
  `score` decimal(18,2) DEFAULT NULL COMMENT '得分',
  `isCancel` tinyint(1) DEFAULT '0' COMMENT '取消',
  `frequency` int(11) DEFAULT NULL COMMENT '次数',
  `teacherId` int(11) DEFAULT NULL COMMENT '老师',
  `markTime` int(11) DEFAULT '0' COMMENT '批阅时间',
  `obScore` decimal(18,2) DEFAULT '0.00' COMMENT '客观题得分',
  `subScore` decimal(18,2) DEFAULT '0.00' COMMENT '主观题得分',
  `markOrder` int(11) DEFAULT '0' COMMENT '改卷顺序',
  `platform` varchar(10) DEFAULT NULL COMMENT '平台',
  `courseId` int(11) DEFAULT '0' COMMENT '课程Id',
  `evalState` int(11) DEFAULT '0' COMMENT '互评状态',
  `markId` int(11) DEFAULT '0' COMMENT '改卷人',
  `classId` int(11) DEFAULT '0' COMMENT '班级Id',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `addDate` date GENERATED ALWAYS AS (date_format(from_unixtime(`startTime`),_utf8mb4'%Y-%m-%d')) VIRTUAL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `workId` (`workId`,`userId`) USING BTREE,
  KEY `schoolId` (`schoolId`) USING BTREE,
  KEY `addDate` (`addDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3298156 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='作业记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_score`
--

DROP TABLE IF EXISTS `yee_work_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_score` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workId` int(11) DEFAULT NULL COMMENT '作业Id',
  `userId` int(11) DEFAULT NULL COMMENT '学生Id',
  `finalScore` decimal(18,2) DEFAULT NULL COMMENT '最终得分',
  `state` int(11) DEFAULT '0' COMMENT '状态',
  `scored` tinyint(1) DEFAULT '0' COMMENT '已打分',
  `submitTime` int(11) DEFAULT '0' COMMENT '提交时间',
  `timeCost` int(11) DEFAULT '0' COMMENT '用时',
  `platform` varchar(10) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '平台',
  `courseId` int(11) DEFAULT '0' COMMENT '课程ID',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `workId` (`workId`,`userId`) USING BTREE,
  KEY `courseId` (`courseId`,`userId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3127021 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='作业分数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yee_work_topic`
--

DROP TABLE IF EXISTS `yee_work_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yee_work_topic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `topic` mediumtext COMMENT '题干',
  `type` int(11) DEFAULT NULL COMMENT '试题类型',
  `level` int(11) DEFAULT NULL COMMENT '难度等级',
  `score` int(11) DEFAULT NULL COMMENT '默认分值',
  `missScore` json DEFAULT NULL COMMENT '漏选分值',
  `option1` json DEFAULT NULL COMMENT '单选选项',
  `option2` json DEFAULT NULL COMMENT '多选选项',
  `option3` json DEFAULT NULL COMMENT '判断选项',
  `analysis` mediumtext COMMENT '题目解析',
  `pid` int(11) DEFAULT NULL COMMENT '父Id',
  `workId` int(11) DEFAULT '0' COMMENT '试卷Id',
  `title` varchar(500) DEFAULT NULL COMMENT '标识',
  `oid` int(11) DEFAULT NULL,
  `number` int(11) DEFAULT '0' COMMENT '序号',
  `upload` varchar(200) DEFAULT NULL COMMENT '上传附件',
  `option` json DEFAULT NULL COMMENT '题目选项',
  `scoreMode` int(11) DEFAULT NULL COMMENT '计分模式',
  `schoolId` int(11) DEFAULT '0' COMMENT '学校Id',
  `categoryId` json DEFAULT NULL COMMENT '学科分类',
  `cateBid` int(11) DEFAULT '0' COMMENT '学科分类',
  `cateMid` int(11) DEFAULT '0' COMMENT '学科分类',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `type` (`type`) USING BTREE,
  KEY `workId` (`workId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1200364 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='作业试题';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- GTID state at the end of the backup 
--

SET @@GLOBAL.GTID_PURGED='12084a8c-d418-11ef-89f0-1c34da52d038:1-160954,
e42ad625-0819-11ee-84dd-b8599faebf74:1-1321579';
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-08-27 14:18:11
