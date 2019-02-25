package com.snow.example.interview;


import java.io.*;
import java.util.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 随机生成 Salary {name, baseSalary, bonus  }的记录，如“wxxx,10,1”，每行一条记录，总共1000万记录，写入文本文件（UFT-8编码），
 *    然后读取文件，name的前两个字符相同的，其年薪累加，比如wx，100万，3个人，最后做排序和分组，输出年薪总额最高的10组：
 *          wx, 200万，10人
 *          lt, 180万，8人
 *          ....
 * name 4位a-z随机，    baseSalary [0,100]随机 bonus[0-5]随机 年薪总额 = baseSalary*13 + bonus
 */
public class SalaryDemo {
    public static void main(String[] args) throws IOException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("d://Salary.txt"));
        CountDownLatch countDownLatch = new CountDownLatch(100);
        //进行写入数据
        for(int i=0 ; i<100; i++){
            executorService.execute(new RandomTask(bufferedWriter,countDownLatch));
        }
        countDownLatch.await();
        //进行读出数据
        BufferedReader bufferedReader = new BufferedReader( new FileReader("d://Salary.txt"));
        String str;
        Set<String> hashSet = new HashSet();
        Set<SalaryInfo> salaryInfoSet = new TreeSet<>();
        Map<String,SalaryInfo> salaryInfoMap =  new HashMap<>();
        try {
            while ((str = bufferedReader.readLine()) != null){
                String[] temp = str.split(",");
                 String namePref =temp[0].substring(0,2);
                 if(hashSet.contains(namePref)){
                    SalaryInfo salaryInfo = salaryInfoMap.get(namePref);
                    salaryInfo.setSalarySum(salaryInfo.getSalarySum()+(Integer.valueOf(temp[1])*13+Integer.valueOf(temp[2])));
                    salaryInfo.setPresonSum(salaryInfo.getPresonSum()+1);
                 }else {
                     hashSet.add(namePref);
                     SalaryInfo salaryInfo = new SalaryInfo();
                     salaryInfo.setNamePref(namePref);
                     salaryInfo.setSalarySum(Integer.valueOf(temp[1])*13+Integer.valueOf(temp[2]));
                     salaryInfo.setPresonSum(1);
                     salaryInfoMap.put(namePref,salaryInfo);
                 }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(HashMap.Entry<String,SalaryInfo> entry : salaryInfoMap.entrySet()){
            salaryInfoSet.add(entry.getValue());
        }
        int index =0;
        for(SalaryInfo salaryInfo: salaryInfoSet){
            if(index >=10){
                break;
            }
            System.out.println(salaryInfo.toString());
            index++;
        }
        executorService.shutdown();
    }
    static class RandomTask implements Runnable{
        BufferedWriter bufferedWriter;
        CountDownLatch countDownLatch;
        public RandomTask(BufferedWriter bufferedWriter,CountDownLatch countDownLatch){
            this.bufferedWriter=bufferedWriter;
            this.countDownLatch=countDownLatch;
        }
        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<10;i++){
                char name1=(char)('a'+Math.random()*('z'-'a'+1));
                char name2=(char)('a'+Math.random()*('z'-'a'+1));
                char name3=(char)('a'+Math.random()*('z'-'a'+1));
                char name4=(char)('a'+Math.random()*('z'-'a'+1));
                int baseSalary =((Double)(Math.random()*100)).intValue();
                int bonus =((Double)(Math.random()*5)).intValue();
                //int annualSalary = baseSalary*13+bonus;
                sb.append(name1).append(name2).append(name3).append(name4).append(",").append(baseSalary).append(",").append(bonus).append("\n");
            }
            try {
                bufferedWriter.write(sb.toString());
                bufferedWriter.flush();
            } catch (IOException e) {
                //根据实际业务进行异常处理
                e.printStackTrace();
            }finally {
                countDownLatch.countDown();
            }

        }
    }
    static class SalaryInfo implements Comparable<SalaryInfo>{
        String namePref;
        int salarySum;
        int presonSum;

        public String getNamePref() {
            return namePref;
        }

        public void setNamePref(String namePre) {
            this.namePref = namePre;
        }

        public int getSalarySum() {
            return salarySum;
        }

        public void setSalarySum(int salarySum) {
            this.salarySum = salarySum;
        }

        public int getPresonSum() {
            return presonSum;
        }

        public void setPresonSum(int presonSum) {
            this.presonSum = presonSum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SalaryInfo that = (SalaryInfo) o;
            return salarySum == that.salarySum &&
                    presonSum == that.presonSum &&
                    Objects.equals(namePref, that.namePref);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namePref, salarySum, presonSum);
        }

        @Override
        public int compareTo(SalaryInfo o) {
            return o.salarySum-this.getSalarySum();
        }

        @Override
        public String toString() {
            return "SalaryInfo{" +
                    "namePref='" + namePref + '\'' +
                    ", salarySum=" + salarySum +
                    ", presonSum=" + presonSum +
                    '}';
        }
    }
}
