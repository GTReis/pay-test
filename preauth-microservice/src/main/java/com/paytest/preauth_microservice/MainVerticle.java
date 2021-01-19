package com.paytest.preauth_microservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class MainVerticle extends AbstractVerticle {

  private WebClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    client = WebClient.create(vertx);
    Router router = Router.router(vertx);
    router.get("/preauthorization").handler(this::PreAuthorization);

    vertx.createHttpServer()
    .requestHandler(router)
    .listen(8082, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8082");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void PrintResult(JsonObject body, RoutingContext rc) {
    JsonObject result = body.getJsonObject("result");
    String codeResult = result.getString("code");

    if(codeResult.contains("000.100.110")) {
      String responseString = "responseID: " + body.getString("id") + " result_code: " + codeResult + " description: " + result.getString("description");
      rc.json(body);
      System.out.println(responseString);
    }
  }

  private void SendResult(JsonObject body) {
    JsonObject result = body.getJsonObject("result");
    String codeResult = result.getString("code");

    if(codeResult.contains("000.100.110")) {
      client.get(8081, "capture-microservice", "/capture")
        .addQueryParam("amount", body.getString("amount"))
        .addQueryParam("id", body.getString("id"))
        .send();
    }
  }

  private void PreAuthorization(RoutingContext rc) {
    MultiMap queryParams = rc.queryParams();
    String brand = queryParams.contains("brand") ? queryParams.get("brand").toUpperCase() : "unknown";
    String amount = queryParams.get("amount");
    
    String entityId = config().getString("entityId");
    String authorization = config().getString("authorization");
		
		MultiMap body = MultiMap.caseInsensitiveMultiMap();
		body.set("entityId", entityId);
		body.set("amount", amount);
		body.set("currency", "BRL");
		body.set("paymentBrand", brand);
		body.set("paymentType", "PA");
		body.set("card.number", "5454545454545454");
		body.set("card.holder", "Jane Jones");
		body.set("card.expiryMonth", "05");
		body.set("card.expiryYear", "2034");
		body.set("card.cvv", "123");
		
		HttpRequest<JsonObject> request = client.postAbs("https://test.oppwa.com/v1/payments").ssl(true)
				.bearerTokenAuthentication(authorization)
				.as(BodyCodec.jsonObject());
		
		
		request.sendForm(body,ar -> {
			if(ar.succeeded()) {
        SendResult(ar.result().body());
        PrintResult(ar.result().body(), rc);
			}
			else {
        rc.response().end(ar.cause().getMessage());
        System.out.println(ar.cause().getMessage());
			}
		});
  }
}
