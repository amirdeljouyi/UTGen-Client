package org.evosuite.llm;
import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.network.http.DefaultHttpEngine;
import com.apollographql.apollo3.rx3.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Single;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.evosuite.llm.type.Response;
import org.evosuite.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;

public class GraphQLClient {
    ApolloClient client;

    public GraphQLClient(){
        ApolloClient.Builder builder = new ApolloClient.Builder()
                .serverUrl("http://0.0.0.0:8000/graphql");

//        // Optionally, set an http cache
//        HttpCache.configureApolloClientBuilder(builder, cacheDirectory, cacheMaxSize);
//
//        // Optionally, set a normalized cache
//        NormalizedCache.configureApolloClientBuilder(
//                builder,
//                new MemoryCacheFactory(10 * 1024 * 1024, -1),
//                TypePolicyCacheKeyGenerator.INSTANCE,
//                FieldPolicyCacheResolver.INSTANCE,
//                false
//        );

        client = builder.build();
//        PromptQuery promptQuery = PromptQuery.builder().build();
    }

    public void promptQuery(String testcase){
        ApolloCall<PromptQuery.Data> queryCall = client.query(new PromptQuery(testcase));
        Single<ApolloResponse<PromptQuery.Data>> queryResponse = Rx3Apollo.single(queryCall);
        queryResponse.subscribe(response -> {
            LoggingUtils.getEvoLogger().info("refined test is:" + response.data);
            client.close();
        });
    }
}
