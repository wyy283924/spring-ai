package com.wyy;

import java.util.*;

public class CosineSimilarity {
    
    // 计算两个向量的点积
    public static double dotProduct(double[] vectorA, double[] vectorB) {
        double dotProduct = 0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
        }
        return dotProduct;
    }
    
    // 计算向量的模
    public static double vectorMagnitude(double[] vector) {
        double magnitude = 0;
        for (double component : vector) {
            magnitude += Math.pow(component, 2);
        }
        return Math.sqrt(magnitude);
    }
    
    // 计算余弦相似度
    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = dotProduct(vectorA, vectorB);
        double magnitudeA = vectorMagnitude(vectorA);
        double magnitudeB = vectorMagnitude(vectorB);
        
        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0; // 避免除以零
        } else {
            return dotProduct / (magnitudeA * magnitudeB);
        }
    }
    
    public static void main(String[] args) {
        // 示例向量
        double[] vectorA = {1, 2, 3};
        double[] vectorB = {3, 2, 1};
        
        // 计算余弦相似度
        double similarity = cosineSimilarity(vectorA, vectorB);
        System.out.println("Cosine Similarity: " + similarity);
    }
}
