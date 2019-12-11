package main

import pb "github.com/GoogleCloudPlatform/microservices-demo/src/checkoutservice/genproto"
import (
	"encoding/json"
	"net/http"
)

import "github.com/sirupsen/logrus"
import "bytes"
import "io/ioutil"

type Device struct {
	UserId    string
	ProductId string
}

type Devices []Device

func registerDevicesFromOrder(req *pb.PlaceOrderRequest, order *pb.OrderResult, log *logrus.Logger) {

	var devices []Device

	for _, orderItem := range order.GetItems() {
		var item = orderItem.Item
		device := Device{
			UserId:    req.Email,
			ProductId: item.ProductId,
		}

		devices = append(devices, device)

	}

	log.Infof("Device registration, prepared payload: %+v", devices)

	devicesJSON, err := json.Marshal(devices)
	if err != nil {
		log.Errorf("Device registration, failed to prepare payload as json, err: %+v", err)
	}

	url := "https://hackathon-sap19-wal-1025.appspot.com/devices"

	httpRequest, httpError := http.NewRequest("POST", url, bytes.NewBuffer(devicesJSON))
	if httpError == nil {
		httpRequest.Header.Set("Content-Type", "application/json")
		httpClient := &http.Client{}

		log.Infof("Device registration, http request: %+v", httpRequest)

		httpResponse, httpResponseError := httpClient.Do(httpRequest)
		if httpResponseError != nil {
			log.Errorf("Device registration, http request preparation failed, err: %+v", httpResponseError)
		}
		log.Infof("Device registration, http response: %+v", httpResponse)

		bodyBytes, errorBody := ioutil.ReadAll(httpResponse.Body)
		if errorBody != nil {
			log.Error(err)
		}
		bodyString := string(bodyBytes)

		log.Infof("Device registration, http response Body: %+v", bodyString)
		defer httpResponse.Body.Close()
	} else {
		log.Errorf("Device registration, http request preparation failed, err: %+v, URL: %+v", httpError, url)
	}

}
