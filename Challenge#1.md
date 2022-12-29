### Prerequisite 
* Running Redis: `docker run -p 6379:6379 -it --rm redis:latest`
* Load data: `java -jar target/redisolar-1.0.jar load`

#### Understand data storage
Sites was stored as:
* Set: 
  * Key `app:sites:ids`
  * Value: 
      ```
        app:sites:info:45
        app:sites:info:102
        app:sites:info:87
        ...
      ```
* Hash: 
  * Key `app:sites:info:115`
    * Values:
    ```
      {
        "id": 45,
        "capacity": 6.0,
        "state": "CA",
        "address": "281 MacArthur Boulevard"
        ...
      }
    ```

### Implement challenge
```agsl
@Override
public Set<Site> findAll() {
   return Collections.emptySet();
}
```

### Idea
To get all Sites from Redis:
1. Get all members from key set
2. Get detail info for each site
