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

import java.util.concurrent.TimeUnit;

public class GraphQLClient {
    ApolloClient client;

    public GraphQLClient(){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES)
                .build();

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

        client = builder.httpEngine(new DefaultHttpEngine(okHttpClient)).build();
    }

    public Single<ApolloResponse<PromptQuery.Data>> promptQuery(String testcase){
        PromptQuery promptQuery = PromptQuery.builder().test(testcase).build();

//        ApolloCall<PromptQuery.Data> queryCall = client.query(new PromptQuery(testcase));
//        Single<ApolloResponse<PromptQuery.Data>> queryResponse = Rx3Apollo.single(queryCall);
        return Rx3Apollo.single(client.query(promptQuery));
    }
}
