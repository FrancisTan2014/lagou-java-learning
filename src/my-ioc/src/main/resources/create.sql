-- create table
CREATE TABLE `Account` (
  `cardNo` varchar(50) NOT NULL,
  `name` varchar(20) NOT NULL,
  `money` int(11) NOT NULL,
  PRIMARY KEY (`cardNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- insert test data
INSERT INTO `Account` VALUES('6029621011001', '韩梅梅', 10000);
INSERT INTO `Account` VALUES('6029621011000', '李大雷', 10000);