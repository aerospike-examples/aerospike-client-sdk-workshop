import React, { useEffect, useState } from "react";
import styles from "./index.module.css";
import logo from "../../assets/logo.png";
import cart from "../../assets/shopping-cart.png";
import Profile from "../profile";
import { useCart } from "../../context/CartContext";
import SearchableDropdown from "../searchableDropdown";
import { apiUrl } from "../../config/api";

const Header = () => {
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState("");
    const [articleType, setArticleType] = useState("");
    const [usage, setUsage] = useState("");
    const [brandName, setBrandName] = useState("");
    const [categories, setCategories] = useState([]);
    const [articleTypes, setArticleTypes] = useState([]);
    const [usageTypes, setUsageTypes] = useState([]);
    const [brandNames, setBrandNames] = useState([]);
    const { getCartItemCount } = useCart();

    useEffect(() => {
        // Load dropdown data from API
        fetchCategories();
        fetchArticleTypes();
        fetchUsageTypes();
        fetchBrandNames();
        
        // Get current URL parameters
        let params = new URLSearchParams(window.location.search);
        let q = params.get("q");
        let cat = params.get("category");
        let art = params.get("articleType");
        let use = params.get("usage");
        let brand = params.get("brandName");
        
        if(q) setQuery(q);
        if(cat) setCategory(cat);
        if(art) setArticleType(art);
        if(use) setUsage(use);
        if(brand) setBrandName(brand);
    }, [])

    const fetchCategories = async () => {
        try {
            const response = await fetch(apiUrl('categories'));
            const data = await response.json();
            setCategories(data.categories || []);
        } catch (error) {
            console.error('Error fetching categories:', error);
        }
    };

    const fetchArticleTypes = async () => {
        try {
            const response = await fetch(apiUrl('article-types'));
            const data = await response.json();
            setArticleTypes(data.articleTypes || []);
        } catch (error) {
            console.error('Error fetching article types:', error);
        }
    };

    const fetchUsageTypes = async () => {
        try {
            const response = await fetch(apiUrl('usage-types'));
            const data = await response.json();
            setUsageTypes(data.usageTypes || []);
        } catch (error) {
            console.error('Error fetching usage types:', error);
        }
    };

    const fetchBrandNames = async () => {
        try {
            const response = await fetch(apiUrl('brand-names'));
            const data = await response.json();
            setBrandNames(data.brandNames || []);
        } catch (error) {
            console.error('Error fetching brand names:', error);
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        
        // Build search URL with parameters
        const params = new URLSearchParams();
        if (query.trim()) params.append('q', query);
        if (category) params.append('category', category);
        if (articleType) params.append('articleType', articleType);
        if (usage) params.append('usage', usage);
        if (brandName) params.append('brandName', brandName);
        
        const searchUrl = `/search${params.toString() ? '?' + params.toString() : ''}`;
        window.location.href = searchUrl;
    };

    return (
        <header className={styles.header}>
            <div className={styles.headContainer}>
                <a href="/"><img src={logo} alt="Logo" className={styles.logo} /></a>
                <nav className={styles.nav}>
                    <form onSubmit={handleSearch} className={styles.searchForm}>
                        <div className={styles.filtersContainer}>
                            <div className={styles.selectWrapper}>
                                <label className={`${styles.floatingLabel} ${category ? styles.floated : ''}`}>
                                    Category
                                </label>
                                <select 
                                    className={styles.dropdown}
                                    value={category}
                                    onChange={(e) => setCategory(e.target.value)}
                                >
                                    <option value=""></option>
                                    {categories.map(cat => (
                                        <option key={cat} value={cat}>{cat}</option>
                                    ))}
                                </select>
                            </div>
                            
                            <div className={styles.selectWrapper}>
                                <label className={`${styles.floatingLabel} ${articleType ? styles.floated : ''}`}>
                                    Article Type
                                </label>
                                <select 
                                    className={styles.dropdown}
                                    value={articleType}
                                    onChange={(e) => setArticleType(e.target.value)}
                                >
                                    <option value=""></option>
                                    {articleTypes.map(type => (
                                        <option key={type} value={type}>{type}</option>
                                    ))}
                                </select>
                            </div>
                            
                            <div className={styles.selectWrapper}>
                                <label className={`${styles.floatingLabel} ${usage ? styles.floated : ''}`}>
                                    Usage
                                </label>
                                <select 
                                    className={styles.dropdown}
                                    value={usage}
                                    onChange={(e) => setUsage(e.target.value)}
                                >
                                    <option value=""></option>
                                    {usageTypes.map(use => (
                                        <option key={use} value={use}>{use}</option>
                                    ))}
                                </select>
                            </div>
                            
                            <div className={styles.selectWrapper}>
                                <SearchableDropdown
                                    options={brandNames}
                                    value={brandName}
                                    onChange={setBrandName}
                                    label="Brand Name"
                                />
                            </div>
                        </div>
                        
                        <input 
                            className={styles.searchHidden} 
                            type='search' 
                            name="q" 
                            placeholder="Search"
                            value={query} 
                            onChange={(e) => setQuery(e.currentTarget.value)}
                            style={{ display: 'none' }} />
                            
                        <button type="submit" className={styles.searchButton}>
                            Search
                        </button>
                    </form>
                </nav>
                <div className={styles.controls}>
                    <Profile />
                    <a href="/cart" className={styles.cartLink}>
                        <img src={cart} className={styles.cart}/>
                        {getCartItemCount() > 0 && (
                            <span className={styles.cartBadge}>
                                {getCartItemCount()}
                            </span>
                        )}
                    </a>
                </div>
            </div>
        </header>
    )
}

export default Header;