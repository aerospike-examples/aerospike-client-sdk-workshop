import { useLoaderData } from "react-router-dom";
import styles from "./index.module.css";
import Card from "../../components/card";
import { apiUrl } from "../../config/api";

export const searchLoader = async ({ request }) => {
    let url = new URL(request.url);
    let query = url.searchParams.get("q");
    let category = url.searchParams.get("category");
    let articleType = url.searchParams.get("articleType");
    let usage = url.searchParams.get("usage");
    let brandName = url.searchParams.get("brandName");
    
    // Build the search URL with all parameters
    const searchParams = new URLSearchParams();
    if (query) searchParams.append("q", query);
    if (category) searchParams.append("category", category);
    if (articleType) searchParams.append("articleType", articleType);
    if (usage) searchParams.append("usage", usage);
    if (brandName) searchParams.append("brandName", brandName);
    
    let response = await fetch(apiUrl(`search?${searchParams.toString()}`));
    let results = await response.json();
    
    return { 
        results, 
        query, 
        category, 
        articleType, 
        usage,
        brandName 
    };
}

export const categoryLoader = async (idx, filter) => {
    let index;
    switch (idx) {
        case "cat":
            index = "category";
            break;
        case "sub":
            index = "subCategory";
            break;
        case "use":
            index = "usage";
            break;
        default:
            index = ""
    }
    console.log(index)
    let response = await fetch(apiUrl(`category?idx=${index}&filter_value=${filter}`));
    
    let results = await response.json();
    return { results, filter };
}

const Products = () => {
    const {results: { products, time }, query = null, category = null, articleType = null, usage = null, brandName = null, filter = null} = useLoaderData();

    return (
        <div className={styles.container}>
            <div className={styles.resultMeta}>
                {query && <span className={styles.filter}>Search: <strong>{query}</strong></span>}
                {category && <span className={styles.filter}>Category: <strong>{category}</strong></span>}
                {articleType && <span className={styles.filter}>Article Type: <strong>{articleType}</strong></span>}
                {usage && <span className={styles.filter}>Usage: <strong>{usage}</strong></span>}
                {brandName && <span className={styles.filter}>Brand: <strong>{brandName}</strong></span>}
                {filter && <span className={styles.filter}>Category <strong>{filter}</strong></span>}
                <span className={styles.time}>Query executed in {time}ms</span>
            </div>
            <div className={styles.products}>
                {products.map(product => (
                    <Card key={product.id} product={product} />
                ))}
            </div>
        </div>
    )
}

export default Products;