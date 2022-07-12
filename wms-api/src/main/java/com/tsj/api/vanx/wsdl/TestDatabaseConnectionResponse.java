
package com.tsj.api.vanx.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>anonymous complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TestDatabaseConnectionResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "testDatabaseConnectionResult"
})
@XmlRootElement(name = "TestDatabaseConnectionResponse")
public class TestDatabaseConnectionResponse {

    @XmlElement(name = "TestDatabaseConnectionResult")
    protected String testDatabaseConnectionResult;

    /**
     * ��ȡtestDatabaseConnectionResult���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTestDatabaseConnectionResult() {
        return testDatabaseConnectionResult;
    }

    /**
     * ����testDatabaseConnectionResult���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTestDatabaseConnectionResult(String value) {
        this.testDatabaseConnectionResult = value;
    }

}
