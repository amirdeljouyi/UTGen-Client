package org.evosuite.llm;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.network.http.DefaultHttpEngine;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
public class GraphQLClient {
    ApolloClient client;

    public GraphQLClient(){
        ApolloClient.Builder builder = new ApolloClient.Builder()
                .serverUrl("http://0.0.0.0:8000/graphql");
        client = builder.build();
//        GetUsersQuery getUsersQuery = GetUsersQuery.builder().build();
    }

    public void randomQuery(){
//        client.query(new GetRandomQuery()).enqueue(response -> {
//            if (response.data != null) {
//                // No errors
//                System.out.println(response.data);
//            } else {
//                // Errors
//                if (response.exception instanceof ApolloGraphQLException) {
//                    // GraphQL errors
//                    System.out.println(((ApolloGraphQLException) response.exception).getErrors().get(0));
//                } else {
//                    // Network error
//                    response.exception.printStackTrace();
//                }
//            }
//        });
    }
}
