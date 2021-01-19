package com.paytest.capture_microservice;

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
    router.get("/capture").handler(this::Capture);
    vertx.createHttpServer().requestHandler(router)
    .listen(8081, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8081");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void PrintResult(JsonObject body) {
    JsonObject result = body.getJsonObject("result");
    String codeResult = result.getString("code");

    if(codeResult.contains("000.100.110")) {
      String responseString = "responseID: " + body.getString("id") + " result_code: " + codeResult + " description: " + result.getString("description");
      System.out.println(responseString);
    }
  }

  private void SendResult(JsonObject body) {
    JsonObject result = body.getJsonObject("result");
    String codeResult = result.getString("code");

    if(codeResult.contains("000.100.110")) {
      client.get(8083, "refund-microservice", "/refund")
        .addQueryParam("amount", body.getString("amount"))
        .addQueryParam("id", body.getString("id"))
        .send();
    }
  }

  private void Capture(RoutingContext rc) {
    MultiMap queryParams = rc.queryParams();

    String paymentId = queryParams.get("id");
    String amount = queryParams.get("amount");

    String entityId = config().getString("entityId");
    String authorization = config().getString("authorization");

    MultiMap body = MultiMap.caseInsensitiveMultiMap();
    body.set("entityId", entityId);
    body.set("amount", amount);
    body.set("paymentType", "CP");
    body.set("currency", "BRL");

    HttpRequest<JsonObject> request = client.postAbs("https://test.oppwa.com/v1/payments/" + paymentId).ssl(true)
				.bearerTokenAuthentication(authorization)
        .as(BodyCodec.jsonObject());
    
    request.sendForm(body, ar -> {
      if(ar.succeeded()) {
        PrintResult(ar.result().body());
        SendResult(ar.result().body());
			}
			else {
        System.out.println(ar.cause().getMessage());
			}
    });
  }
}
